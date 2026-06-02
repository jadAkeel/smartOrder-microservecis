package com.jadakeel.inventory.repository;

import com.jadakeel.inventory.model.InventoryReservation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends MongoRepository<InventoryReservation, UUID> {
    Optional<InventoryReservation> findByOrderId(UUID orderId);
}