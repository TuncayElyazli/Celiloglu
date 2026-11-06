package com.celiloglu.backend.dto.request;

import java.math.BigDecimal;

public class ProductFilterRequestDto {

    private String code;
    private Long categoryId;
    private Long carBrandId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer year;

    public ProductFilterRequestDto() {
    }

    public ProductFilterRequestDto(String code, Long categoryId, Long carBrandId,
            BigDecimal minPrice, BigDecimal maxPrice, Integer year) {
        this.code = code;
        this.categoryId = categoryId;
        this.carBrandId = carBrandId;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.year = year;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getCarBrandId() {
        return carBrandId;
    }

    public void setCarBrandId(Long carBrandId) {
        this.carBrandId = carBrandId;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }
}
