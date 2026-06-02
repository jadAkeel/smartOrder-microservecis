package com.jadakeel.inventory.controller;

import com.jadakeel.common.dto.OrderItemDto;
import com.jadakeel.common.event.order.OrderCreatedEvent;
import com.jadakeel.inventory.dto.InventoryItemRequest;
import com.jadakeel.inventory.dto.InventoryItemResponse;
import com.jadakeel.inventory.dto.InventoryReservationRequest;
import com.jadakeel.inventory.dto.OrderInventoryRequest;
import com.jadakeel.inventory.model.InventoryItem;
import com.jadakeel.inventory.service.InventoryService;
import com.jadakeel.inventory.service.InventoryItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryItemService inventoryItemService;
    private final InventoryService inventoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<InventoryItemResponse> createInventoryItem(@Valid @RequestBody InventoryItemRequest request) {
        InventoryItem savedItem = inventoryItemService.createInventoryItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(savedItem));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItemResponse> getInventoryItemById(@PathVariable UUID id) {
        InventoryItem item = inventoryItemService.getInventoryItemById(id);
        return ResponseEntity.ok(mapToResponse(item));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<InventoryItemResponse> getInventoryByProductId(@PathVariable UUID productId) {
        InventoryItem item = inventoryItemService.getInventoryItemById(productId);
        return ResponseEntity.ok(mapToResponse(item));
    }

    @GetMapping
    public ResponseEntity<List<InventoryItemResponse>> getAllInventoryItems() {
        List<InventoryItem> items = inventoryItemService.getAllInventoryItems();
        List<InventoryItemResponse> responseItems = items.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseItems);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryItemResponse> updateInventoryItem(
            @PathVariable UUID id,
            @Valid @RequestBody InventoryItemRequest request) {
        InventoryItem updatedItem = inventoryItemService.updateInventoryItem(id, request);
        return ResponseEntity.ok(mapToResponse(updatedItem));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventoryItem(@PathVariable UUID id) {
        inventoryItemService.deleteInventoryItem(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> checkInventoryAvailability(
            @RequestParam UUID productId,
            @RequestParam Integer quantity) {
        boolean isAvailable = inventoryItemService.checkAvailability(productId, quantity);
        return ResponseEntity.ok(isAvailable);
    }

    @PostMapping("/reserve")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reserveInventory(@Valid @RequestBody InventoryReservationRequest request) {
        UUID orderId = request.getOrderId() == null ? UUID.randomUUID() : request.getOrderId();
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                orderId,
                request.getCustomerId(),
                request.getItems().stream()
                        .map(item -> new OrderItemDto(item.getProductId(), item.getQuantity(), item.getPrice()))
                        .collect(Collectors.toList()),
                request.getTotalAmount()
        );
        inventoryService.reserveInventory(event);
    }

    @PostMapping("/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseReservation(@Valid @RequestBody OrderInventoryRequest request) {
        inventoryService.cancelReservation(request.getOrderId());
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmReservation(@Valid @RequestBody OrderInventoryRequest request) {
        inventoryService.confirmReservation(request.getOrderId());
    }

    private InventoryItemResponse mapToResponse(InventoryItem item) {
        return InventoryItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .availableQuantity(item.getAvailableQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .build();
    }
}
