package com.jadakeel.shipping.controller;

import com.jadakeel.shipping.dto.DeliveryAssignRequest;
import com.jadakeel.shipping.model.Shipment;
import com.jadakeel.shipping.service.ShippingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final ShippingService shippingService;

    @PostMapping("/assign")
    public ResponseEntity<Shipment> assignDelivery(@Valid @RequestBody DeliveryAssignRequest request) {
        return ResponseEntity.ok(shippingService.assignDelivery(request.getOrderId()));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Shipment> getDeliveryByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(shippingService.getShipmentByOrderId(orderId));
    }

    @PutMapping("/{deliveryId}/status")
    public ResponseEntity<Shipment> updateDeliveryStatus(
            @PathVariable UUID deliveryId,
            @RequestParam Shipment.ShipmentStatus status) {
        return ResponseEntity.ok(shippingService.updateStatus(deliveryId, status));
    }

    @PutMapping("/{deliveryId}/location")
    public ResponseEntity<Shipment> updateDeliveryLocation(
            @PathVariable UUID deliveryId,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(shippingService.updateLocation(deliveryId, request.get("currentLocation")));
    }
}
