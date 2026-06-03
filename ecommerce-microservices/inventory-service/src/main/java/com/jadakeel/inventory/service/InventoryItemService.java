package com.jadakeel.inventory.service;

import com.jadakeel.inventory.dto.InventoryItemRequest;
import com.jadakeel.inventory.model.InventoryItem;

import java.util.List;
import java.util.UUID;

public interface InventoryItemService {
    InventoryItem createInventoryItem(InventoryItemRequest request);
    InventoryItem getInventoryItemById(UUID id);
    List<InventoryItem> getAllInventoryItems();
    InventoryItem updateInventoryItem(UUID id, InventoryItemRequest request);
    void deleteInventoryItem(UUID id);
    InventoryItem getInventoryItemByProductId(UUID productId);
    boolean checkAvailability(UUID productId, Integer quantity);
}