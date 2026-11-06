package com.celiloglu.backend.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartResponseDto {

    private List<CartItemResponseDto> items = new ArrayList<>();
    private Long totalItems = 0L;
    private BigDecimal totalPrice = BigDecimal.ZERO;

    public CartResponseDto() {}

    public CartResponseDto(List<CartItemResponseDto> items, Long totalItems, BigDecimal totalPrice) {
        this.items = items != null ? items : new ArrayList<>();
        this.totalItems = totalItems != null ? totalItems : 0L;
        this.totalPrice = totalPrice != null ? totalPrice : BigDecimal.ZERO;
    }

    public List<CartItemResponseDto> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponseDto> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public Long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Long totalItems) {
        this.totalItems = totalItems;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
