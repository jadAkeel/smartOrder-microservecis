package com.jadakeel.shipping.service;

import com.jadakeel.common.event.payment.PaymentProcessedEvent;
import com.jadakeel.shipping.model.Shipment;

import java.util.UUID;

public interface ShippingService {
    void processShipping(PaymentProcessedEvent paymentEvent);
    Shipment getShipmentByOrderId(UUID orderId);
    Shipment getShipmentById(UUID shipmentId);
    Shipment assignDelivery(UUID orderId);
    Shipment updateStatus(UUID shipmentId, Shipment.ShipmentStatus status);
    Shipment updateLocation(UUID shipmentId, String currentLocation);
}
