package com.jadakeel.payment.service.impl;

import com.jadakeel.common.event.inventory.InventoryReservedEvent;
import com.jadakeel.common.event.payment.PaymentFailedEvent;
import com.jadakeel.common.event.payment.PaymentProcessedEvent;
import com.jadakeel.payment.dto.PaymentRequest;
import com.jadakeel.payment.model.Payment;
import com.jadakeel.payment.repository.PaymentRepository;
import com.jadakeel.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public void processPayment(InventoryReservedEvent event) {
        log.info("Processing payment for order: {}", event.getOrderId());

        // In a real application, we would call a payment gateway
        // Here we'll simulate payment processing with random success/failure

        // First, get the order details (in a real app this would come from a service call or the event)
        UUID orderId = event.getOrderId();
        UUID customerId = event.getCustomerId() == null ? UUID.randomUUID() : event.getCustomerId();
        BigDecimal amount = event.getTotalAmount() == null ? BigDecimal.ZERO : event.getTotalAmount();

        payInternal(orderId, customerId, event.getCorrelationId(), amount, "CREDIT_CARD", null);
    }

    @Override
    @Transactional
    public Payment pay(PaymentRequest request) {
        return payInternal(
                request.getOrderId(),
                request.getCustomerId(),
                UUID.randomUUID(),
                request.getAmount(),
                request.getPaymentMethod() == null ? "CREDIT_CARD" : request.getPaymentMethod(),
                request.getSimulateSuccess()
        );
    }

    private Payment payInternal(UUID orderId, UUID customerId, UUID correlationId, BigDecimal amount,
                                String paymentMethod, Boolean simulateSuccess) {
        Payment payment = null;
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent()) {
            payment = existingPayment.get();
            if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
                log.info("Returning existing successful payment for order: {}", orderId);
                return payment;
            }
            if (payment.getStatus() == Payment.PaymentStatus.PENDING) {
                log.info("Returning existing pending payment for order: {}", orderId);
                return payment;
            }
        }

        if (payment == null) {
            payment = Payment.builder()
                    .orderId(orderId)
                    .customerId(customerId)
                    .correlationId(correlationId)
                    .amount(amount)
                    .paymentMethod(paymentMethod)
                    .build();
        }

        payment.setCustomerId(customerId);
        payment.setCorrelationId(correlationId);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(Payment.PaymentStatus.PENDING);

        // Simulate payment processing
        boolean paymentSuccessful = simulateSuccess == null ? simulatePaymentGateway() : simulateSuccess;

        if (paymentSuccessful) {
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setTransactionId(generateTransactionId());
            paymentRepository.save(payment);

            // Send payment success event
            PaymentProcessedEvent processedEvent = new PaymentProcessedEvent(
                    correlationId,
                    orderId,
                    payment.getId()
            );

            kafkaTemplate.send("payment-events", processedEvent);
            log.info("Payment successful for order: {}, transaction ID: {}",
                    orderId, payment.getTransactionId());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);

            // Send payment failure event
            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                    correlationId,
                    orderId,
                    "Payment processing failed"
            );

            kafkaTemplate.send("payment-events", failedEvent);
            log.error("Payment failed for order: {}", orderId);
        }

        return payment;
    }

    @Override
    public Payment getPaymentByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
    }

    private boolean simulatePaymentGateway() {
        // Simulate 90% success rate
        return new Random().nextInt(10) < 9;
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
