package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.request.ProductFilterRequestDto;
import com.celiloglu.backend.dto.request.ProductRequestDto;
import com.celiloglu.backend.dto.response.ProductResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    Page<ProductResponseDto> getAllProducts(Pageable pageable);

    ProductResponseDto getProductById(Long id);

    Page<ProductResponseDto> getActiveProducts(Pageable pageable);

    ProductResponseDto getActiveProductById(Long id);

    Page<ProductResponseDto> searchActiveProducts(
            ProductFilterRequestDto filter, Pageable pageable);

    ProductResponseDto createProduct(ProductRequestDto requestDto);

    ProductResponseDto updateProduct(
            Long id, ProductRequestDto requestDto);

    void updateStockQuantity(Long productId, Integer newStock, Integer expectedStock);

    void deleteProduct(Long id);
}
