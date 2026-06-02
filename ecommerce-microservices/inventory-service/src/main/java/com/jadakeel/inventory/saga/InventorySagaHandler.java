package com.jadakeel.inventory.saga;

import com.jadakeel.common.event.order.OrderCreatedEvent;
import com.jadakeel.common.event.payment.PaymentFailedEvent;
import com.jadakeel.common.event.payment.PaymentProcessedEvent;
import com.jadakeel.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventorySagaHandler {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "order-events", groupId = "inventory-service-group")
    public void handleOrderEvents(Object event) {
        if (event instanceof OrderCreatedEvent orderCreatedEvent) {
            log.info("Received OrderCreatedEvent for order: {}", orderCreatedEvent.getOrderId());
            inventoryService.reserveInventory(orderCreatedEvent);
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "inventory-service-group")
    public void handlePaymentEvents(Object event) {
        if (event instanceof PaymentProcessedEvent paymentProcessedEvent) {
            log.info("Received PaymentProcessedEvent for order: {}, confirming inventory reservation",
                    paymentProcessedEvent.getOrderId());
            inventoryService.confirmReservation(paymentProcessedEvent.getOrderId());
        } else if (event instanceof PaymentFailedEvent paymentFailedEvent) {
            log.info("Received PaymentFailedEvent for order: {}, cancelling inventory reservation",
                    paymentFailedEvent.getOrderId());
            inventoryService.cancelReservation(paymentFailedEvent.getOrderId());
        }
    }
}
