package com.jadakeel.order.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadakeel.common.event.inventory.InventoryReservationFailedEvent;
import com.jadakeel.common.event.inventory.InventoryReservedEvent;
import com.jadakeel.common.event.payment.PaymentFailedEvent;
import com.jadakeel.common.event.payment.PaymentProcessedEvent;
import com.jadakeel.common.event.shipping.ShipmentFailedEvent;
import com.jadakeel.common.event.shipping.ShipmentProcessedEvent;
import com.jadakeel.common.util.KafkaEventUtils;
import com.jadakeel.order.model.Order;
import com.jadakeel.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaHandler {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory-events", groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleInventoryEvents(Object event) {
        log.info("Received inventory event: {}", event.getClass().getSimpleName());

        if (hasField(event, "totalAmount")) {
            InventoryReservedEvent reservedEvent = convertEvent(event, InventoryReservedEvent.class);
            // Inventory was successfully reserved
            orderService.updateOrderStatus(reservedEvent.getOrderId(), Order.OrderStatus.INVENTORY_RESERVED);
            log.info("Inventory reserved for order: {}", reservedEvent.getOrderId());
        } else if (hasField(event, "reason")) {
            InventoryReservationFailedEvent failedEvent = convertEvent(event, InventoryReservationFailedEvent.class);
            // Inventory reservation failed - cancel order
            orderService.updateOrderStatus(failedEvent.getOrderId(), Order.OrderStatus.CANCELLED);
            log.error("Inventory reservation failed for order: {}, reason: {}",
                    failedEvent.getOrderId(), failedEvent.getReason());
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handlePaymentEvents(Object event) {
        log.info("Received payment event: {}", event.getClass().getSimpleName());

        if (hasField(event, "paymentId")) {
            PaymentProcessedEvent processedEvent = convertEvent(event, PaymentProcessedEvent.class);
            // Payment was successfully processed
            orderService.updateOrderStatus(processedEvent.getOrderId(), Order.OrderStatus.PAID);
            log.info("Payment processed for order: {}", processedEvent.getOrderId());
        } else if (hasField(event, "reason")) {
            PaymentFailedEvent failedEvent = convertEvent(event, PaymentFailedEvent.class);
            // Payment failed - cancel order
            orderService.updateOrderStatus(failedEvent.getOrderId(), Order.OrderStatus.FAILED);
            log.error("Payment failed for order: {}, reason: {}",
                    failedEvent.getOrderId(), failedEvent.getReason());
        }
    }

    @KafkaListener(topics = "shipping-events", groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleShippingEvents(Object event) {
        log.info("Received shipping event: {}", event.getClass().getSimpleName());

        if (hasField(event, "trackingNumber")) {
            ShipmentProcessedEvent processedEvent = convertEvent(event, ShipmentProcessedEvent.class);
            // Shipment was successfully processed
            orderService.updateOrderStatus(processedEvent.getOrderId(), Order.OrderStatus.OUT_FOR_DELIVERY);
            log.info("Order shipped: {}, tracking number: {}",
                    processedEvent.getOrderId(), processedEvent.getTrackingNumber());
        } else if (hasField(event, "reason")) {
            ShipmentFailedEvent failedEvent = convertEvent(event, ShipmentFailedEvent.class);
            // Shipping failed
            orderService.updateOrderStatus(failedEvent.getOrderId(), Order.OrderStatus.FAILED);
            log.error("Shipping failed for order: {}, reason: {}",
                    failedEvent.getOrderId(), failedEvent.getReason());
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
