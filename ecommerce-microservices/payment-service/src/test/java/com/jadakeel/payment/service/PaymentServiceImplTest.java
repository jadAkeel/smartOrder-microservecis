package com.jadakeel.payment.service;

import com.jadakeel.common.event.inventory.InventoryReservedEvent;
import com.jadakeel.payment.dto.PaymentRequest;
import com.jadakeel.payment.model.Payment;
import com.jadakeel.payment.repository.PaymentRepository;
import com.jadakeel.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentRepository, kafkaTemplate);
    }

    @Test
    void processPayment_shouldSucceedAndPublishEvent() {
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(
                UUID.randomUUID(), orderId, UUID.randomUUID(), BigDecimal.valueOf(50.00)
        );
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.processPayment(event);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertEquals(orderId, saved.getOrderId());
        assertEquals(BigDecimal.valueOf(50.00), saved.getAmount());

        verify(kafkaTemplate, times(1)).send(eq("payment-events"), any());
    }

    @Test
    void payment_shouldBeIdempotent() {
        UUID orderId = UUID.randomUUID();
        Payment existingPayment = Payment.builder()
                .orderId(orderId)
                .status(Payment.PaymentStatus.SUCCESS)
                .build();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingPayment));

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setCustomerId(UUID.randomUUID());
        request.setAmount(BigDecimal.valueOf(100));
        request.setSimulateSuccess(false);

        Payment result = paymentService.pay(request);

        assertEquals(Payment.PaymentStatus.SUCCESS, result.getStatus());
        // Should not save again or publish events
        verify(paymentRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    void refund_shouldChangeStatusToRefunded() {
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .status(Payment.PaymentStatus.SUCCESS)
                .amount(BigDecimal.valueOf(50))
                .build();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Payment result = paymentService.refund(orderId);

        assertEquals(Payment.PaymentStatus.REFUNDED, result.getStatus());
        assertTrue(result.getTransactionId().startsWith("REFUND-"));
        verify(kafkaTemplate, times(1)).send(eq("payment-events"), any());
    }

    @Test
    void refund_shouldThrowWhenPaymentNotFound() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.refund(orderId));
    }

    @Test
    void refund_shouldThrowWhenNotSuccessful() {
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .orderId(orderId)
                .status(Payment.PaymentStatus.FAILED)
                .build();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class, () -> paymentService.refund(orderId));
    }

    @Test
    void getPaymentByOrderId_shouldReturnPayment() {
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.builder().orderId(orderId).build();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentByOrderId(orderId);

        assertEquals(orderId, result.getOrderId());
    }

    @Test
    void getPaymentByOrderId_shouldThrowWhenNotFound() {
        when(paymentRepository.findByOrderId(any())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.getPaymentByOrderId(UUID.randomUUID()));
    }
}
