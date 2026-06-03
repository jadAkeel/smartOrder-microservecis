package com.jadakeel.shipping.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipments", uniqueConstraints = @UniqueConstraint(name = "uk_shipments_order_id", columnNames = "order_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    private UUID customerId;
    private UUID correlationId;

    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;

    private String trackingNumber;
    private String carrierName;
    private LocalDateTime shippedDate;
    private LocalDateTime estimatedDeliveryDate;
    private String currentLocation;

    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime lastModifiedAt;
    private String lastModifiedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
        this.createdBy = "system";
        this.lastModifiedBy = "system";
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedAt = LocalDateTime.now();
        this.lastModifiedBy = "system";
    }

    public enum ShipmentStatus {
        DRIVER_ASSIGNED, PICKED_UP, OUT_FOR_DELIVERY, DELIVERED, FAILED
    }
}
