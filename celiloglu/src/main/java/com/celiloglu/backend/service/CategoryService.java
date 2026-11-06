package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.request.CategoryRequestDto;
import com.celiloglu.backend.dto.response.CategoryDto;

import java.util.List;

public interface CategoryService {

    List<CategoryDto> getAllCategories();

    CategoryDto getCategoryById(Long id);

    CategoryDto createCategory(CategoryRequestDto requestDto);

    CategoryDto updateCategory(Long id, CategoryRequestDto requestDto);

    void deleteCategory(Long id);
}
