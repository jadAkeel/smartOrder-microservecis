package com.jadakeel.shipping.service.impl;

import com.jadakeel.common.event.payment.PaymentProcessedEvent;
import com.jadakeel.common.event.shipping.ShipmentFailedEvent;
import com.jadakeel.common.event.shipping.ShipmentProcessedEvent;
import com.jadakeel.shipping.model.Shipment;
import com.jadakeel.shipping.repository.ShipmentRepository;
import com.jadakeel.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingServiceImpl implements ShippingService {

    private final ShipmentRepository shipmentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String[] CARRIERS = {"DHL", "FedEx", "UPS", "USPS"};

    @Override
    @Transactional
    public void processShipping(PaymentProcessedEvent paymentEvent) {
        log.info("Processing shipping for order: {}", paymentEvent.getOrderId());

        var existingShipment = shipmentRepository.findByOrderId(paymentEvent.getOrderId());
        if (existingShipment.isPresent()) {
            log.info("Shipment already exists for order: {}, tracking: {}",
                    paymentEvent.getOrderId(), existingShipment.get().getTrackingNumber());
            publishShipmentProcessed(paymentEvent, existingShipment.get());
            return;
        }

        UUID customerId = paymentEvent.getCustomerId() != null
                ? paymentEvent.getCustomerId()
                : UUID.randomUUID();

        try {
            Shipment shipment = Shipment.builder()
                    .orderId(paymentEvent.getOrderId())
                    .customerId(customerId)
                    .correlationId(paymentEvent.getCorrelationId())
                    .status(Shipment.ShipmentStatus.DRIVER_ASSIGNED)
                    .carrierName(getRandomCarrier())
                    .trackingNumber(generateTrackingNumber())
                    .shippedDate(LocalDateTime.now())
                    .estimatedDeliveryDate(LocalDateTime.now().plusDays(3))
                    .currentLocation("Assigned")
                    .shippingAddress("123 Main St, New York, NY 10001")
                    .recipientName("John Doe")
                    .recipientPhone("(212) 555-1234")
                    .build();

            Shipment savedShipment = shipmentRepository.save(shipment);

            savedShipment.setStatus(Shipment.ShipmentStatus.OUT_FOR_DELIVERY);
            shipmentRepository.save(savedShipment);

            publishShipmentProcessed(paymentEvent, savedShipment);
            log.info("Order shipped successfully. Order ID: {}, Tracking: {}",
                    paymentEvent.getOrderId(), savedShipment.getTrackingNumber());

        } catch (Exception e) {
            log.error("Failed to process shipping for order: {}", paymentEvent.getOrderId(), e);

            ShipmentFailedEvent failedEvent = new ShipmentFailedEvent(
                    paymentEvent.getCorrelationId(),
                    paymentEvent.getOrderId(),
                    customerId,
                    "Failed to process shipment: " + e.getMessage()
            );

            kafkaTemplate.send("shipping-events", failedEvent);
        }
    }

    @Override
    public Shipment getShipmentByOrderId(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Shipment not found for order: " + orderId));
    }

    @Override
    public Shipment getShipmentById(UUID shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found with ID: " + shipmentId));
    }

    @Override
    @Transactional
    public Shipment assignDelivery(UUID orderId) {
        processShipping(new PaymentProcessedEvent(UUID.randomUUID(), orderId, UUID.randomUUID()));
        return getShipmentByOrderId(orderId);
    }

    @Override
    @Transactional
    public Shipment updateStatus(UUID shipmentId, Shipment.ShipmentStatus status) {
        Shipment shipment = getShipmentById(shipmentId);
        shipment.setStatus(status);
        return shipmentRepository.save(shipment);
    }

    @Override
    @Transactional
    public Shipment updateLocation(UUID shipmentId, String currentLocation) {
        Shipment shipment = getShipmentById(shipmentId);
        shipment.setCurrentLocation(currentLocation);
        return shipmentRepository.save(shipment);
    }

    private String getRandomCarrier() {
        return CARRIERS[new Random().nextInt(CARRIERS.length)];
    }

    private String generateTrackingNumber() {
        // Format: 2 letters + 9 digits + 2 letters
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();

        Random random = new Random();

        // First 2 letters
        for (int i = 0; i < 2; i++) {
            sb.append(letters.charAt(random.nextInt(letters.length())));
        }

        // 9 digits
        for (int i = 0; i < 9; i++) {
            sb.append(random.nextInt(10));
        }

        // Last 2 letters
        for (int i = 0; i < 2; i++) {
            sb.append(letters.charAt(random.nextInt(letters.length())));
        }

        return sb.toString();
    }

    private void publishShipmentProcessed(PaymentProcessedEvent paymentEvent, Shipment shipment) {
        ShipmentProcessedEvent shipmentEvent = new ShipmentProcessedEvent(
                paymentEvent.getCorrelationId(),
                paymentEvent.getOrderId(),
                shipment.getCustomerId(),
                shipment.getId(),
                shipment.getTrackingNumber()
        );

        kafkaTemplate.send("shipping-events", shipmentEvent);
    }

}
