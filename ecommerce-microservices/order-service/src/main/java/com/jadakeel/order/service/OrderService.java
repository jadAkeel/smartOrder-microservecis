package com.jadakeel.order.service;

import com.jadakeel.order.dto.OrderRequest;
import com.jadakeel.order.dto.OrderResponse;
import com.jadakeel.order.model.Order;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(OrderRequest orderRequest);
    OrderResponse getOrderById(UUID orderId);
    List<OrderResponse> getAllOrders();
    List<OrderResponse> getOrdersByCustomerId(UUID customerId);
    void updateOrderStatus(UUID orderId, Order.OrderStatus status);
}