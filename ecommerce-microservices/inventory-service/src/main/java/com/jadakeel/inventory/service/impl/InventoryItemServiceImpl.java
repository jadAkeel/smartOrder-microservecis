package com.jadakeel.inventory.service.impl;

import com.jadakeel.inventory.dto.InventoryItemRequest;
import com.jadakeel.inventory.model.InventoryItem;
import com.jadakeel.inventory.repository.InventoryRepository;
import com.jadakeel.inventory.service.InventoryItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryItemServiceImpl implements InventoryItemService {

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public InventoryItem createInventoryItem(InventoryItemRequest request) {
        if (inventoryRepository.existsByProductId(request.getProductId())) {
            throw new IllegalArgumentException("Inventory item already exists for product: " + request.getProductId());
        }
        InventoryItem item = InventoryItem.builder()
                .id(UUID.randomUUID())
                .productId(request.getProductId())
                .name(request.getName())
                .description(request.getDescription())
                .availableQuantity(request.getQuantity())
                .reservedQuantity(0)
                .build();

        log.info("Creating new inventory item: {} for product: {}", item.getName(), request.getProductId());
        return inventoryRepository.save(item);
    }

    @Override
    public InventoryItem getInventoryItemById(UUID id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found with id: " + id));
    }

    @Override
    public List<InventoryItem> getAllInventoryItems() {
        return inventoryRepository.findAll();
    }

    @Override
    @Transactional
    public InventoryItem updateInventoryItem(UUID id, InventoryItemRequest request) {
        InventoryItem existingItem = getInventoryItemById(id);

        existingItem.setName(request.getName());
        existingItem.setDescription(request.getDescription());
        existingItem.setAvailableQuantity(request.getQuantity());

        log.info("Updating inventory item: {}", existingItem.getId());
        return inventoryRepository.save(existingItem);
    }

    @Override
    @Transactional
    public void deleteInventoryItem(UUID id) {
        InventoryItem item = getInventoryItemById(id);
        log.info("Deleting inventory item: {}", id);
        inventoryRepository.delete(item);
    }

    @Override
    public InventoryItem getInventoryItemByProductId(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory item not found for product: " + productId));
    }

    @Override
    public boolean checkAvailability(UUID productId, Integer quantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElse(null);

        if (item == null) {
            return false;
        }

        return quantity != null
                && quantity > 0
                && item.getAvailableQuantity() != null
                && item.getAvailableQuantity() >= quantity;
    }
}
