package com.jadakeel.payment.service;

import com.jadakeel.common.event.inventory.InventoryReservedEvent;
import com.jadakeel.payment.dto.PaymentRequest;
import com.jadakeel.payment.model.Payment;

import java.util.UUID;

public interface PaymentService {
    void processPayment(InventoryReservedEvent event);
    Payment pay(PaymentRequest request);
    Payment getPaymentByOrderId(UUID orderId);
    Payment refund(UUID orderId);
}
