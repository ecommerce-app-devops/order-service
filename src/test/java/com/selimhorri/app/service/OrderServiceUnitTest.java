package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.impl.OrderServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private Cart testCart;
    private OrderDto testOrderDto;

    @BeforeEach
    void setUp() {
        testCart = Cart.builder()
                .cartId(1)
                .build();

        testOrder = Order.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test order")
                .orderFee(100.0)
                .status(OrderStatus.CREATED)
                .isActive(true)
                .cart(testCart)
                .build();

        CartDto cartDto = CartDto.builder()
                .cartId(1)
                .build();

        testOrderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test order")
                .orderFee(100.0)
                .orderStatus(OrderStatus.CREATED)
                .cartDto(cartDto)
                .build();
    }

    @Test
    @DisplayName("Should find order by ID when order exists and is active")
    void testFindById_Success() {
        // Given
        Integer orderId = 1;
        when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(testOrder));

        // When
        OrderDto result = orderService.findById(orderId);

        // Then
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals("Test order", result.getOrderDesc());
        verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void testFindById_OrderNotFound() {
        // Given
        Integer orderId = 999;
        when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(OrderNotFoundException.class, () -> orderService.findById(orderId));
        verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
    }

    @Test
    @DisplayName("Should find all active orders")
    void testFindAll_Success() {
        // Given
        List<Order> orders = new ArrayList<>();
        orders.add(testOrder);
        
        Order anotherOrder = Order.builder()
                .orderId(2)
                .orderDate(LocalDateTime.now())
                .orderDesc("Another order")
                .orderFee(200.0)
                .status(OrderStatus.ORDERED)
                .isActive(true)
                .cart(testCart)
                .build();
        orders.add(anotherOrder);

        when(orderRepository.findAllByIsActiveTrue()).thenReturn(orders);

        // When
        List<OrderDto> result = orderService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderRepository, times(1)).findAllByIsActiveTrue();
    }

    @Test
    @DisplayName("Should save order successfully when cart exists")
    void testSave_Success() {
        // Given
        OrderDto newOrderDto = OrderDto.builder()
                .orderDesc("New order")
                .orderFee(150.0)
                .cartDto(CartDto.builder().cartId(1).build())
                .build();

        when(cartRepository.findById(1)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        OrderDto result = orderService.save(newOrderDto);

        // Then
        assertNotNull(result);
        verify(cartRepository, times(1)).findById(1);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when saving order without cart")
    void testSave_WithoutCart() {
        // Given
        OrderDto newOrderDto = OrderDto.builder()
                .orderDesc("New order")
                .orderFee(150.0)
                .cartDto(null)
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> orderService.save(newOrderDto));
        verify(cartRepository, never()).findById(anyInt());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when cart not found")
    void testSave_CartNotFound() {
        // Given
        OrderDto newOrderDto = OrderDto.builder()
                .orderDesc("New order")
                .orderFee(150.0)
                .cartDto(CartDto.builder().cartId(999).build())
                .build();

        when(cartRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CartNotFoundException.class, () -> orderService.save(newOrderDto));
        verify(cartRepository, times(1)).findById(999);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should update order status from CREATED to ORDERED")
    void testUpdateStatus_CreatedToOrdered() {
        // Given
        Integer orderId = 1;
        testOrder.setStatus(OrderStatus.CREATED);
        when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        OrderDto result = orderService.updateStatus(orderId);

        // Then
        assertNotNull(result);
        verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should update order status from ORDERED to IN_PAYMENT")
    void testUpdateStatus_OrderedToInPayment() {
        // Given
        Integer orderId = 1;
        testOrder.setStatus(OrderStatus.ORDERED);
        when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        OrderDto result = orderService.updateStatus(orderId);

        // Then
        assertNotNull(result);
        verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when updating status of order in payment")
    void testUpdateStatus_InPayment_ThrowsException() {
        // Given
        Integer orderId = 1;
        testOrder.setStatus(OrderStatus.IN_PAYMENT);
        when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(IllegalStateException.class, () -> orderService.updateStatus(orderId));
        verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should delete order successfully when status allows")
    void testDeleteById_Success() {
        // Given
        Integer orderId = 1;
        testOrder.setStatus(OrderStatus.CREATED);
        when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        orderService.deleteById(orderId);

        // Then
        verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
        assertFalse(testOrder.isActive());
    }

    @Test
    @DisplayName("Should throw exception when deleting order in payment")
    void testDeleteById_InPayment_ThrowsException() {
        // Given
        Integer orderId = 1;
        testOrder.setStatus(OrderStatus.IN_PAYMENT);
        when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(IllegalStateException.class, () -> orderService.deleteById(orderId));
        verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should update order successfully")
    void testUpdate_Success() {
        // Given
        Integer orderId = 1;
        OrderDto updatedOrderDto = OrderDto.builder()
                .orderId(1)
                .orderDesc("Updated order")
                .orderFee(200.0)
                .build();

        when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        OrderDto result = orderService.update(orderId, updatedOrderDto);

        // Then
        assertNotNull(result);
        verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
    }
}

