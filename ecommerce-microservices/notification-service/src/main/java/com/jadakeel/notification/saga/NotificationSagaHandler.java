package com.jadakeel.notification.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadakeel.common.event.order.OrderCreatedEvent;
import com.jadakeel.common.event.payment.PaymentFailedEvent;
import com.jadakeel.common.event.payment.PaymentProcessedEvent;
import com.jadakeel.common.event.shipping.ShipmentProcessedEvent;
import com.jadakeel.common.util.KafkaEventUtils;
import com.jadakeel.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSagaHandler {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "notification-service-group")
    public void handleOrderEvents(Object event) {
        OrderCreatedEvent orderCreatedEvent = convertEvent(event, OrderCreatedEvent.class);
        log.info("Received OrderCreatedEvent for order: {}", orderCreatedEvent.getOrderId());
        notificationService.sendOrderCreatedNotification(
                orderCreatedEvent.getOrderId(),
                orderCreatedEvent.getCustomerId()
        );
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service-group")
    public void handlePaymentEvents(Object event) {
        if (hasField(event, "paymentId")) {
            PaymentProcessedEvent paymentProcessedEvent = convertEvent(event, PaymentProcessedEvent.class);
            log.info("Received PaymentProcessedEvent for order: {}", paymentProcessedEvent.getOrderId());
            UUID customerId = paymentProcessedEvent.getCustomerId() != null
                    ? paymentProcessedEvent.getCustomerId()
                    : paymentProcessedEvent.getOrderId();
            notificationService.sendPaymentSuccessNotification(
                    paymentProcessedEvent.getOrderId(),
                    customerId
            );
        } else if (hasField(event, "reason")) {
            PaymentFailedEvent paymentFailedEvent = convertEvent(event, PaymentFailedEvent.class);
            log.info("Received PaymentFailedEvent for order: {}", paymentFailedEvent.getOrderId());
            UUID customerId = paymentFailedEvent.getCustomerId() != null
                    ? paymentFailedEvent.getCustomerId()
                    : paymentFailedEvent.getOrderId();
            notificationService.sendPaymentFailedNotification(
                    paymentFailedEvent.getOrderId(),
                    customerId
            );
        }
    }

    @KafkaListener(topics = "shipping-events", groupId = "notification-service-group")
    public void handleShippingEvents(Object event) {
        if (hasField(event, "trackingNumber")) {
            ShipmentProcessedEvent shipmentProcessedEvent = convertEvent(event, ShipmentProcessedEvent.class);
            log.info("Received ShipmentProcessedEvent for order: {}", shipmentProcessedEvent.getOrderId());
            UUID customerId = shipmentProcessedEvent.getCustomerId() != null
                    ? shipmentProcessedEvent.getCustomerId()
                    : shipmentProcessedEvent.getOrderId();
            notificationService.sendOrderShippedNotification(
                    shipmentProcessedEvent.getOrderId(),
                    customerId,
                    shipmentProcessedEvent.getTrackingNumber()
            );
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
