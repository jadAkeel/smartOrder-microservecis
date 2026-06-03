package com.jadakeel.payment.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadakeel.common.event.inventory.InventoryReservedEvent;
import com.jadakeel.common.event.shipping.ShipmentFailedEvent;
import com.jadakeel.common.util.KafkaEventUtils;
import com.jadakeel.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaHandler {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory-events", groupId = "payment-service-group")
    public void handleInventoryEvents(Object event) {
        if (hasField(event, "totalAmount")) {
            InventoryReservedEvent inventoryReservedEvent = convertEvent(event, InventoryReservedEvent.class);
            log.info("Received InventoryReservedEvent for order: {}", inventoryReservedEvent.getOrderId());
            paymentService.processPayment(inventoryReservedEvent);
        }
    }

    @KafkaListener(topics = "shipping-events", groupId = "payment-service-group")
    public void handleShippingEvents(Object event) {
        if (hasField(event, "reason")) {
            ShipmentFailedEvent shipmentFailedEvent = convertEvent(event, ShipmentFailedEvent.class);
            log.info("Received ShipmentFailedEvent for order: {}, initiating refund",
                    shipmentFailedEvent.getOrderId());
            try {
                paymentService.refund(shipmentFailedEvent.getOrderId());
                log.info("Refund processed for order: {}", shipmentFailedEvent.getOrderId());
            } catch (Exception ex) {
                log.error("Failed to refund payment for order: {}: {}",
                        shipmentFailedEvent.getOrderId(), ex.getMessage());
            }
        }
    }

    private <T> T convertEvent(Object event, Class<T> eventType) {
        Object payload = payload(event);
        if (eventType.isInstance(payload)) {
            return eventType.cast(payload);
        }
        return objectMapper.convertValue(payload, eventType);
    }

    private boolean hasField(Object event, String fieldName) {
        return KafkaEventUtils.hasField(event, fieldName);
    }

    private Object payload(Object event) {
        return KafkaEventUtils.payload(event);
    }
}
