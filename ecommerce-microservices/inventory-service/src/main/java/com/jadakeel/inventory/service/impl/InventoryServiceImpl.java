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

    @Override
    @Transactional
    public void reserveInventory(OrderCreatedEvent orderCreatedEvent) {
        log.info("Processing inventory reservation for order: {}", orderCreatedEvent.getOrderId());

        List<InventoryReservation.ReservationItem> reservationItems = new ArrayList<>();
        boolean allItemsAvailable = true;
        StringBuilder insufficientItemsMessage = new StringBuilder();

        // Check if all items are available in sufficient quantity
        for (var orderItem : orderCreatedEvent.getItems()) {
            InventoryItem item = inventoryRepository.findById(orderItem.getProductId())
                    .orElse(null);

            if (item == null) {
                allItemsAvailable = false;
                insufficientItemsMessage.append("Product not found: ")
                        .append(orderItem.getProductId()).append("; ");
                continue;
            }

            int availableQuantity = item.getAvailableQuantity() == null ? 0 : item.getAvailableQuantity();

            if (availableQuantity < orderItem.getQuantity()) {
                allItemsAvailable = false;
                insufficientItemsMessage.append("Insufficient quantity for product: ")
                        .append(orderItem.getProductId())
                        .append(", requested: ").append(orderItem.getQuantity())
                        .append(", available: ").append(availableQuantity)
                        .append("; ");
                continue;
            }

            // Add to reservation items
            reservationItems.add(InventoryReservation.ReservationItem.builder()
                    .productId(orderItem.getProductId())
                    .quantity(orderItem.getQuantity())
                    .build());
        }

        if (!allItemsAvailable) {
            // Send failure event
            InventoryReservationFailedEvent failedEvent = new InventoryReservationFailedEvent(
                    orderCreatedEvent.getCorrelationId(),
                    orderCreatedEvent.getOrderId(),
                    insufficientItemsMessage.toString()
            );

            kafkaTemplate.send("inventory-events", failedEvent);
            log.error("Inventory reservation failed: {}", insufficientItemsMessage);
            return;
        }

        // Create reservation
        InventoryReservation reservation = InventoryReservation.builder()
                .id(UUID.randomUUID())
                .orderId(orderCreatedEvent.getOrderId())
                .correlationId(orderCreatedEvent.getCorrelationId())
                .items(reservationItems)
                .status(InventoryReservation.ReservationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        reservationRepository.save(reservation);

        // Update inventory quantities
        for (var reservationItem : reservationItems) {
            InventoryItem item = inventoryRepository.findById(reservationItem.getProductId()).orElseThrow();
            int availableQuantity = item.getAvailableQuantity() == null ? 0 : item.getAvailableQuantity();
            int reservedQuantity = item.getReservedQuantity() == null ? 0 : item.getReservedQuantity();
            item.setAvailableQuantity(availableQuantity - reservationItem.getQuantity());
            item.setReservedQuantity(reservedQuantity + reservationItem.getQuantity());
            inventoryRepository.save(item);
        }

        // Send success event
        InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                orderCreatedEvent.getCorrelationId(),
                orderCreatedEvent.getOrderId(),
                orderCreatedEvent.getCustomerId(),
                orderCreatedEvent.getTotalAmount()
        );

        kafkaTemplate.send("inventory-events", reservedEvent);
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
            InventoryItem item = inventoryRepository.findById(reservationItem.getProductId()).orElseThrow();
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
            InventoryItem item = inventoryRepository.findById(reservationItem.getProductId()).orElseThrow();
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
}
