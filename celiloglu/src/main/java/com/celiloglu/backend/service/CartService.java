package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.request.CartItemRequestDto;
import com.celiloglu.backend.dto.response.CartResponseDto;

public interface CartService {

    Long getCartTotalItems(Long userId);

    CartResponseDto getCart(Long userId);

    CartResponseDto addItemToCart(Long userId, CartItemRequestDto request);

    CartResponseDto addItemByCode(Long userId, String code, Integer quantity);

    CartResponseDto updateItemQuantity(Long userId, Long productId, Integer quantity);

    CartResponseDto removeItemFromCart(Long userId, Long productId);

    void clearCart(Long userId);
}

