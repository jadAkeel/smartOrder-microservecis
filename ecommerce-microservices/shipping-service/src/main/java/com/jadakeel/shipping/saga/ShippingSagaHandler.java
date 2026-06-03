package com.jadakeel.shipping.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadakeel.common.event.payment.PaymentProcessedEvent;
import com.jadakeel.common.util.KafkaEventUtils;
import com.jadakeel.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShippingSagaHandler {

    private final ShippingService shippingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-events", groupId = "shipping-service-group")
    public void handlePaymentEvents(Object event) {
        if (hasField(event, "paymentId")) {
            PaymentProcessedEvent paymentProcessedEvent = convertEvent(event, PaymentProcessedEvent.class);
            log.info("Received PaymentProcessedEvent for order: {}, processing shipment",
                    paymentProcessedEvent.getOrderId());
            shippingService.processShipping(paymentProcessedEvent);
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
