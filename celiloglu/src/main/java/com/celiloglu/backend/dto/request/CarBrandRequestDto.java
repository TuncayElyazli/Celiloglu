package com.celiloglu.backend.dto.request;

public class CarBrandRequestDto {

    private String name;

    public CarBrandRequestDto() {
    }

    public CarBrandRequestDto(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
