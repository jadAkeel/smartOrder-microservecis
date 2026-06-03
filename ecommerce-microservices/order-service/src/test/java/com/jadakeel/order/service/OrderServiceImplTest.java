package com.jadakeel.order.service;

import com.jadakeel.order.dto.OrderItemRequest;
import com.jadakeel.order.dto.OrderRequest;
import com.jadakeel.order.dto.OrderResponse;
import com.jadakeel.order.model.Order;
import com.jadakeel.order.model.OrderItem;
import com.jadakeel.order.repository.OrderRepository;
import com.jadakeel.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, kafkaTemplate);
    }

    @Test
    void createOrder_shouldSaveOrderAndPublishEvent() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(UUID.randomUUID());
        itemRequest.setProductName("Test Product");
        itemRequest.setQuantity(2);
        itemRequest.setPrice(BigDecimal.valueOf(10.00));

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setCustomerId(UUID.randomUUID());
        orderRequest.setItems(List.of(itemRequest));

        OrderItem savedItem = OrderItem.builder()
                .productId(itemRequest.getProductId())
                .productName(itemRequest.getProductName())
                .quantity(2)
                .price(BigDecimal.valueOf(10.00))
                .build();

        Order savedOrder = Order.builder()
                .id(UUID.randomUUID())
                .customerId(orderRequest.getCustomerId())
                .totalAmount(BigDecimal.valueOf(20.00))
                .status(Order.OrderStatus.CREATED)
                .items(List.of(savedItem))
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder(orderRequest);

        assertNotNull(response);
        assertEquals(Order.OrderStatus.CREATED, response.getStatus());
        assertEquals(BigDecimal.valueOf(20.00), response.getTotalAmount());

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(kafkaTemplate, times(1)).send(eq("order-events"), any());
    }

    @Test
    void getOrderById_shouldReturnOrderWhenFound() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .customerId(UUID.randomUUID())
                .totalAmount(BigDecimal.valueOf(15.00))
                .status(Order.OrderStatus.CREATED)
                .items(Collections.emptyList())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.getOrderId());
    }

    @Test
    void getOrderById_shouldThrowWhenNotFound() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> orderService.getOrderById(UUID.randomUUID()));
    }

    @Test
    void getOrdersByCustomerId_shouldReturnOrders() {
        UUID customerId = UUID.randomUUID();
        Order order1 = Order.builder().id(UUID.randomUUID()).customerId(customerId).items(Collections.emptyList()).build();
        Order order2 = Order.builder().id(UUID.randomUUID()).customerId(customerId).items(Collections.emptyList()).build();

        when(orderRepository.findByCustomerId(customerId)).thenReturn(List.of(order1, order2));

        List<OrderResponse> orders = orderService.getOrdersByCustomerId(customerId);

        assertEquals(2, orders.size());
    }

    @Test
    void updateOrderStatus_shouldUpdateStatus() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .status(Order.OrderStatus.CREATED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.updateOrderStatus(orderId, Order.OrderStatus.PAID);

        assertEquals(Order.OrderStatus.PAID, order.getStatus());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void updateOrderStatus_shouldThrowWhenOrderNotFound() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> orderService.updateOrderStatus(UUID.randomUUID(), Order.OrderStatus.FAILED));
    }
}
