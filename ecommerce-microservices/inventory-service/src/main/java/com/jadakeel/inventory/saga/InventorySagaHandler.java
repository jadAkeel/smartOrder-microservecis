package com.jadakeel.inventory.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadakeel.common.event.order.OrderCreatedEvent;
import com.jadakeel.common.event.payment.PaymentFailedEvent;
import com.jadakeel.common.event.payment.PaymentProcessedEvent;
import com.jadakeel.common.util.KafkaEventUtils;
import com.jadakeel.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventorySagaHandler {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "inventory-service-group")
    public void handleOrderEvents(Object event) {
        OrderCreatedEvent orderCreatedEvent = convertEvent(event, OrderCreatedEvent.class);
        log.info("Received OrderCreatedEvent for order: {}", orderCreatedEvent.getOrderId());
        inventoryService.reserveInventory(orderCreatedEvent);
    }

    @KafkaListener(topics = "payment-events", groupId = "inventory-service-group")
    public void handlePaymentEvents(Object event) {
        if (hasField(event, "transactionId")) {
            PaymentProcessedEvent paymentProcessedEvent = convertEvent(event, PaymentProcessedEvent.class);
            log.info("Received PaymentProcessedEvent for order: {}, confirming inventory reservation",
                    paymentProcessedEvent.getOrderId());
            inventoryService.confirmReservation(paymentProcessedEvent.getOrderId());
        } else if (hasField(event, "reason")) {
            PaymentFailedEvent paymentFailedEvent = convertEvent(event, PaymentFailedEvent.class);
            log.info("Received PaymentFailedEvent for order: {}, cancelling inventory reservation",
                    paymentFailedEvent.getOrderId());
            inventoryService.cancelReservation(paymentFailedEvent.getOrderId());
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
