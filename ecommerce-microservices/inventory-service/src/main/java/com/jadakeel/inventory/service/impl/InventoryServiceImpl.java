package com.jadakeel.inventory.service.impl;

import com.jadakeel.common.event.inventory.InventoryReservationFailedEvent;
import com.jadakeel.common.event.inventory.InventoryReservedEvent;
import com.jadakeel.common.event.order.OrderCreatedEvent;
import com.jadakeel.inventory.model.InventoryItem;
import com.jadakeel.inventory.model.InventoryReservation;
import com.jadakeel.inventory.repository.InventoryRepository;
import com.jadakeel.inventory.repository.ReservationRepository;
import com.jadakeel.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MongoTemplate mongoTemplate;

    @Override
    @Transactional
    public void reserveInventory(OrderCreatedEvent orderCreatedEvent) {
        log.info("Processing inventory reservation for order: {}", orderCreatedEvent.getOrderId());

        var existingReservation = reservationRepository.findByOrderId(orderCreatedEvent.getOrderId());
        if (existingReservation.isPresent()) {
            log.info("Reservation already exists for order: {} with status {}",
                    orderCreatedEvent.getOrderId(), existingReservation.get().getStatus());
            if (existingReservation.get().getStatus() != InventoryReservation.ReservationStatus.CANCELLED) {
                publishReservationSuccess(orderCreatedEvent);
            }
            return;
        }

        List<InventoryReservation.ReservationItem> reservationItems = new ArrayList<>();
        List<InventoryReservation.ReservationItem> appliedReservations = new ArrayList<>();
        StringBuilder insufficientItemsMessage = new StringBuilder();

        for (var orderItem : orderCreatedEvent.getItems()) {
            InventoryReservation.ReservationItem reservationItem = InventoryReservation.ReservationItem.builder()
                    .productId(orderItem.getProductId())
                    .quantity(orderItem.getQuantity())
                    .build();

            InventoryItem reservedItem = reserveAvailableQuantity(reservationItem);
            if (reservedItem == null) {
                rollbackReservations(appliedReservations);
                insufficientItemsMessage.append("Insufficient quantity or missing product: ")
                        .append(orderItem.getProductId())
                        .append(", requested: ").append(orderItem.getQuantity())
                        .append("; ");
                break;
            }

            reservationItems.add(reservationItem);
            appliedReservations.add(reservationItem);
        }

        if (reservationItems.size() != orderCreatedEvent.getItems().size()) {
            InventoryReservationFailedEvent failedEvent = new InventoryReservationFailedEvent(
                    orderCreatedEvent.getCorrelationId(),
                    orderCreatedEvent.getOrderId(),
                    insufficientItemsMessage.toString()
            );

            kafkaTemplate.send("inventory-events", failedEvent);
            log.error("Inventory reservation failed: {}", insufficientItemsMessage);
            return;
        }

        InventoryReservation reservation = InventoryReservation.builder()
                .id(UUID.randomUUID())
                .orderId(orderCreatedEvent.getOrderId())
                .correlationId(orderCreatedEvent.getCorrelationId())
                .items(reservationItems)
                .status(InventoryReservation.ReservationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        try {
            reservationRepository.save(reservation);
        } catch (RuntimeException ex) {
            rollbackReservations(appliedReservations);
            throw ex;
        }
        publishReservationSuccess(orderCreatedEvent);
        log.info("Inventory successfully reserved for order: {}", orderCreatedEvent.getOrderId());
    }

    @Override
    @Transactional
    public void confirmReservation(UUID orderId) {
        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Reservation not found for order: " + orderId));

        if (reservation.getStatus() == InventoryReservation.ReservationStatus.CONFIRMED) {
            log.info("Reservation for order {} already confirmed", orderId);
            return;
        }

        reservation.setStatus(InventoryReservation.ReservationStatus.CONFIRMED);
        reservation.setUpdatedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        for (var reservationItem : reservation.getItems()) {
            InventoryItem item = inventoryRepository.findByProductId(reservationItem.getProductId()).orElseThrow();
            int currentReserved = item.getReservedQuantity() == null ? 0 : item.getReservedQuantity();
            item.setReservedQuantity(Math.max(0, currentReserved - reservationItem.getQuantity()));
            inventoryRepository.save(item);
        }

        log.info("Reservation confirmed for order: {}", orderId);
    }

    @Override
    @Transactional
    public void cancelReservation(UUID orderId) {
        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Reservation not found for order: " + orderId));

        if (reservation.getStatus() == InventoryReservation.ReservationStatus.CANCELLED) {
            log.info("Reservation for order {} already cancelled", orderId);
            return;
        }

        // Return quantities back to available inventory
        for (var reservationItem : reservation.getItems()) {
            InventoryItem item = inventoryRepository.findByProductId(reservationItem.getProductId()).orElseThrow();
            item.setAvailableQuantity(item.getAvailableQuantity() + reservationItem.getQuantity());
            int currentReserved = item.getReservedQuantity() == null ? 0 : item.getReservedQuantity();
            item.setReservedQuantity(Math.max(0, currentReserved - reservationItem.getQuantity()));
            inventoryRepository.save(item);
        }

        reservation.setStatus(InventoryReservation.ReservationStatus.CANCELLED);
        reservation.setUpdatedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        log.info("Reservation cancelled and inventory released for order: {}", orderId);
    }

    private InventoryItem reserveAvailableQuantity(InventoryReservation.ReservationItem reservationItem) {
        Query query = Query.query(Criteria.where("productId").is(reservationItem.getProductId())
                .and("availableQuantity").gte(reservationItem.getQuantity()));
        Update update = new Update()
                .inc("availableQuantity", -reservationItem.getQuantity())
                .inc("reservedQuantity", reservationItem.getQuantity());
        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                InventoryItem.class
        );
    }

    private void rollbackReservations(List<InventoryReservation.ReservationItem> appliedReservations) {
        for (var reservationItem : appliedReservations) {
            Query query = Query.query(Criteria.where("productId").is(reservationItem.getProductId()));
            Update update = new Update()
                    .inc("availableQuantity", reservationItem.getQuantity())
                    .inc("reservedQuantity", -reservationItem.getQuantity());
            mongoTemplate.updateFirst(query, update, InventoryItem.class);
        }
    }

    private void publishReservationSuccess(OrderCreatedEvent orderCreatedEvent) {
        InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                orderCreatedEvent.getCorrelationId(),
                orderCreatedEvent.getOrderId(),
                orderCreatedEvent.getCustomerId(),
                orderCreatedEvent.getTotalAmount()
        );

        kafkaTemplate.send("inventory-events", reservedEvent);
    }
}
