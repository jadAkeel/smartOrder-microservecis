package com.jadakeel.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationRequest {
    private UUID orderId;

    @NotNull
    private UUID customerId;

    @NotEmpty
    @Valid
    private List<ReservationItemRequest> items;

    @PositiveOrZero
    private BigDecimal totalAmount;
}
