package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.response.OrderResponseDto;
import com.celiloglu.backend.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    Page<OrderResponseDto> getUserOrders(Long userId, Pageable pageable);
    OrderResponseDto getOrderDetails(Long userId, Long orderId);
    OrderResponseDto createOrderFromCart(Long userId, String shippingAddress, String orderNotes);
    void cancelOrder(Long userId, Long orderId);

    Page<OrderResponseDto> getAllOrdersForAdmin(Pageable pageable);
    OrderResponseDto getOrderDetailsForAdmin(Long orderId);
    void updateOrderStatusForAdmin(Long orderId, String targetStatus);
    void updateOrderStatusForAdmin(Long orderId, OrderStatus targetStatus);
}

