package com.celiloglu.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CartItemRequestDto {

    @NotNull(message = "Məhsul ID-si qeyd edilməlidir")
    private Long productId;

    @NotNull(message = "Məhsul sayı boş ola bilməz")
    @Min(value = 1, message = "Məhsul sayı ən azı 1 olmalıdır")
    private Integer quantity;

    public CartItemRequestDto() {}

    public CartItemRequestDto(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
