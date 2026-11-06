package com.celiloglu.backend.dto.request;

public class CategoryRequestDto {

    private String name;
    private String description;

    public CategoryRequestDto() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
