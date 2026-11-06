package com.celiloglu.backend.dto.response;

import com.celiloglu.backend.util.ProductCodeUtils;

import java.math.BigDecimal;

public class CartItemResponseDto {

    private Long id;
    private Long productId;
    private String productName;
    private String productCode;
    private String productImageUrl;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalPrice;
    private Integer stockQuantity;
    private Boolean available = true;
    private String availabilityMessage;

    public CartItemResponseDto() {}

    public CartItemResponseDto(Long id, Long productId, String productName, String productCode,
                               String productImageUrl, BigDecimal unitPrice, Integer quantity, BigDecimal totalPrice) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productCode = ProductCodeUtils.normalizeCode(productCode);
        this.productImageUrl = productImageUrl;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.available = true;
    }

    public CartItemResponseDto(Long id, Long productId, String productName, String productCode,
                               String productImageUrl, BigDecimal unitPrice, Integer quantity, BigDecimal totalPrice,
                               Boolean available, String availabilityMessage) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productCode = ProductCodeUtils.normalizeCode(productCode);
        this.productImageUrl = productImageUrl;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.available = available != null ? available : true;
        this.availabilityMessage = availabilityMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = ProductCodeUtils.normalizeCode(productCode);
    }

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Boolean getAvailable() {
        return available;
    }

    public Boolean isAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getAvailabilityMessage() {
        return availabilityMessage;
    }

    public void setAvailabilityMessage(String availabilityMessage) {
        this.availabilityMessage = availabilityMessage;
    }
}
