package com.jadakeel.inventory.repository;

import com.jadakeel.inventory.model.InventoryItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends MongoRepository<InventoryItem, UUID> {
    Optional<InventoryItem> findByProductId(UUID productId);
    boolean existsByProductId(UUID productId);
}
