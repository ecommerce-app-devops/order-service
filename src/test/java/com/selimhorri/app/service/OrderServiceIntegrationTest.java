package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.impl.OrderServiceImpl;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("OrderService Integration Tests")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        cartRepository.deleteAll();
    }

    @Test
    @DisplayName("Should integrate with CartRepository to validate cart existence")
    void testSave_IntegrationWithCartRepository() {
        // Given
        Cart cart = Cart.builder()
                .cartId(1)
                .build();
        cart = cartRepository.save(cart);

        OrderDto orderDto = OrderDto.builder()
                .orderDesc("Test order")
                .orderFee(100.0)
                .cartDto(CartDto.builder().cartId(cart.getCartId()).build())
                .build();

        // When
        OrderDto result = orderService.save(orderDto);

        // Then
        assertNotNull(result);
        assertNotNull(result.getOrderId());
        assertEquals("Test order", result.getOrderDesc());
        assertEquals(100.0, result.getOrderFee());
        assertNotNull(result.getCartDto());
    }

    @Test
    @DisplayName("Should integrate with OrderRepository to persist order status changes")
    void testUpdateStatus_IntegrationWithOrderRepository() {
        // Given
        Cart cart = Cart.builder().cartId(1).build();
        cart = cartRepository.save(cart);

        Order order = Order.builder()
                .orderDate(LocalDateTime.now())
                .orderDesc("Test order")
                .orderFee(100.0)
                .status(OrderStatus.CREATED)
                .isActive(true)
                .cart(cart)
                .build();
        order = orderRepository.save(order);

        // When
        OrderDto result = orderService.updateStatus(order.getOrderId());

        // Then
        assertNotNull(result);
        Order updatedOrder = orderRepository.findByOrderIdAndIsActiveTrue(order.getOrderId()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.ORDERED, updatedOrder.getStatus());
    }

    @Test
    @DisplayName("Should integrate with database to find all active orders")
    void testFindAll_IntegrationWithDatabase() {
        // Given
        Cart cart = Cart.builder().cartId(1).build();
        cart = cartRepository.save(cart);

        Order order1 = Order.builder()
                .orderDate(LocalDateTime.now())
                .orderDesc("Order 1")
                .orderFee(100.0)
                .status(OrderStatus.CREATED)
                .isActive(true)
                .cart(cart)
                .build();

        Order order2 = Order.builder()
                .orderDate(LocalDateTime.now())
                .orderDesc("Order 2")
                .orderFee(200.0)
                .status(OrderStatus.ORDERED)
                .isActive(true)
                .cart(cart)
                .build();

        Order order3 = Order.builder()
                .orderDate(LocalDateTime.now())
                .orderDesc("Order 3")
                .orderFee(300.0)
                .status(OrderStatus.CREATED)
                .isActive(false) // Inactive order
                .cart(cart)
                .build();

        orderRepository.save(order1);
        orderRepository.save(order2);
        orderRepository.save(order3);

        // When
        List<OrderDto> result = orderService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size()); // Only active orders
    }

    @Test
    @DisplayName("Should integrate with database for soft delete functionality")
    void testDeleteById_IntegrationWithDatabase_SoftDelete() {
        // Given
        Cart cart = Cart.builder().cartId(1).build();
        cart = cartRepository.save(cart);

        Order order = Order.builder()
                .orderDate(LocalDateTime.now())
                .orderDesc("Test order")
                .orderFee(100.0)
                .status(OrderStatus.CREATED)
                .isActive(true)
                .cart(cart)
                .build();
        order = orderRepository.save(order);

        Integer orderId = order.getOrderId();

        // When
        orderService.deleteById(orderId);

        // Then
        Order deletedOrder = orderRepository.findById(orderId).orElse(null);
        assertNotNull(deletedOrder);
        assertFalse(deletedOrder.isActive());
        // Verify it's not returned in findAll
        List<OrderDto> allOrders = orderService.findAll();
        assertTrue(allOrders.stream().noneMatch(o -> o.getOrderId().equals(orderId)));
    }

    @Test
    @DisplayName("Should integrate with database to update order while preserving cart relationship")
    void testUpdate_IntegrationWithDatabase_PreservesCart() {
        // Given
        Cart cart = Cart.builder().cartId(1).build();
        cart = cartRepository.save(cart);

        Order order = Order.builder()
                .orderDate(LocalDateTime.now())
                .orderDesc("Original order")
                .orderFee(100.0)
                .status(OrderStatus.CREATED)
                .isActive(true)
                .cart(cart)
                .build();
        order = orderRepository.save(order);

        OrderDto updatedOrderDto = OrderDto.builder()
                .orderDesc("Updated order")
                .orderFee(200.0)
                .build();

        // When
        OrderDto result = orderService.update(order.getOrderId(), updatedOrderDto);

        // Then
        assertNotNull(result);
        Order updatedOrder = orderRepository.findByOrderIdAndIsActiveTrue(order.getOrderId()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals("Updated order", updatedOrder.getOrderDesc());
        assertEquals(200.0, updatedOrder.getOrderFee());
        assertNotNull(updatedOrder.getCart()); // Cart relationship preserved
        assertEquals(cart.getCartId(), updatedOrder.getCart().getCartId());
    }
}

