package com.jadakeel.shipping.service;

import com.jadakeel.common.event.payment.PaymentProcessedEvent;
import com.jadakeel.shipping.model.Shipment;
import com.jadakeel.shipping.repository.ShipmentRepository;
import com.jadakeel.shipping.service.impl.ShippingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ShippingService shippingService;

    @BeforeEach
    void setUp() {
        shippingService = new ShippingServiceImpl(shipmentRepository, kafkaTemplate);
    }

    @Test
    void processShipping_republishesExistingShipmentWithoutCreatingDuplicate() {
        UUID orderId = UUID.randomUUID();
        Shipment existingShipment = Shipment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .trackingNumber("AB123456789CD")
                .build();
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                existingShipment.getCustomerId()
        );

        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingShipment));

        shippingService.processShipping(event);

        verify(shipmentRepository, never()).save(any(Shipment.class));
        verify(kafkaTemplate).send(eq("shipping-events"), any());
    }
}
