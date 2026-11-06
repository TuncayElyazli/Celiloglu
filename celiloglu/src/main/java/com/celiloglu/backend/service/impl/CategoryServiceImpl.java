package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.dto.request.CategoryRequestDto;
import com.celiloglu.backend.dto.response.CategoryDto;
import com.celiloglu.backend.entity.Category;
import com.celiloglu.backend.entity.Product;
import com.celiloglu.backend.repository.CategoryRepository;
import com.celiloglu.backend.repository.ProductRepository;
import com.celiloglu.backend.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            ProductRepository productRepository) {

        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToDto)
                .sorted(Comparator.comparing(CategoryDto::getName,
                        String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(Long id) {
        Category category = findCategory(id);
        return mapToDto(category);
    }

    @Override
    @Transactional
    public CategoryDto createCategory(CategoryRequestDto requestDto) {
        validateAndCheckDuplicate(requestDto, null);

        Category category = new Category();
        category.setName(requestDto.getName().trim());
        category.setDescription(requestDto.getDescription());

        Category savedCategory = categoryRepository.save(category);
        return mapToDto(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequestDto requestDto) {
        Category category = findCategory(id);

        validateAndCheckDuplicate(requestDto, id);

        category.setName(requestDto.getName().trim());
        category.setDescription(requestDto.getDescription());

        Category updatedCategory = categoryRepository.save(category);
        return mapToDto(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategory(id);

        List<Product> products = productRepository.findByCategoryId(id);

        if (!products.isEmpty()) {
            throw new IllegalStateException(
                    "Bu kateqoriyaya aid məhsullar mövcuddur, "
                            + "ona görə silinə bilməz!");
        }

        categoryRepository.delete(category);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Kateqoriya tapılmadı! ID: " + id));
    }

    private void validateAndCheckDuplicate(
            CategoryRequestDto requestDto, Long currentId) {

        if (requestDto.getName() == null
                || requestDto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Kateqoriyanın adı boş ola bilməz!");
        }

        String trimmedName = requestDto.getName().trim();

        Category existing = categoryRepository.findByName(trimmedName)
                .orElse(null);

        // Redaktə zamanı kateqoriyanın öz adına icazə verilir.
        if (existing != null
                && !existing.getId().equals(currentId)) {
            throw new IllegalArgumentException(
                    "Bu adla kateqoriya artıq mövcuddur!");
        }
    }

    private CategoryDto mapToDto(Category category) {
        int productCount = productRepository
                .findByCategoryId(category.getId()).size();

        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                productCount);
    }
}
