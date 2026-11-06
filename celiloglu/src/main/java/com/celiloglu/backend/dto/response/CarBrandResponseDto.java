package com.celiloglu.backend.dto.response;

public class CarBrandResponseDto {

    private Long id;
    private String name;
    private Integer productCount;

    public CarBrandResponseDto() {
    }

    public CarBrandResponseDto(Long id, String name, Integer productCount) {
        this.id = id;
        this.name = name;
        this.productCount = productCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getProductCount() {
        return productCount;
    }

    public void setProductCount(Integer productCount) {
        this.productCount = productCount;
    }
}
