package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.dto.request.ProductFilterRequestDto;
import com.celiloglu.backend.dto.request.ProductRequestDto;
import com.celiloglu.backend.dto.response.ProductResponseDto;
import com.celiloglu.backend.entity.CarBrand;
import com.celiloglu.backend.entity.Category;
import com.celiloglu.backend.entity.Product;
import com.celiloglu.backend.repository.CarBrandRepository;
import com.celiloglu.backend.repository.CategoryRepository;
import com.celiloglu.backend.repository.ProductRepository;
import com.celiloglu.backend.repository.specification.ProductSpecifications;
import com.celiloglu.backend.service.ProductService;
import com.celiloglu.backend.util.ProductCodeUtils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CarBrandRepository carBrandRepository;

    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            CarBrandRepository carBrandRepository) {

        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.carBrandRepository = carBrandRepository;
    }

    @Override
    public Page<ProductResponseDto> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::mapToResponseDto);
    }

    @Override
    public ProductResponseDto getProductById(Long id) {
        Product product = findProduct(id);
        return mapToResponseDto(product);
    }

    @Override
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        Product product = new Product();

        applyRequestToProduct(requestDto, product);

        Product savedProduct = productRepository.save(product);
        return mapToResponseDto(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(
            Long id, ProductRequestDto requestDto) {

        Product product = findProduct(id);

        applyRequestToProduct(requestDto, product);

        Product updatedProduct = productRepository.save(product);
        return mapToResponseDto(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProduct(id);

        // Bazadan silmirik, məhsulu deaktiv edirik.
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void updateStockQuantity(Long productId, Integer newStock, Integer expectedStock) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Məhsul ID-si müsbət ədəd olmalıdır!");
        }
        if (newStock == null) {
            throw new IllegalArgumentException("Yeni stok miqdarı qeyd edilməlidir!");
        }
        if (newStock < 0) {
            throw new IllegalArgumentException("Stok miqdarı mənfi ola bilməz!");
        }

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new EntityNotFoundException("Məhsul tapılmadı! ID: " + productId));

        if (expectedStock != null && !java.util.Objects.equals(product.getStockQuantity(), expectedStock)) {
            throw new IllegalStateException(String.format(
                    "Stok miqdarı dəyişdirilib! Aktual stok: %d, gözlənilən: %d. Zəhmət olmasa səhifəni yeniləyib təkrar cəhd edin.",
                    product.getStockQuantity() != null ? product.getStockQuantity() : 0, expectedStock
            ));
        }

        product.setStockQuantity(newStock);
        productRepository.save(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Məhsul tapılmadı! ID: " + id));
    }

    // Həm create, həm update eyni sahələri doldurur.
    private void applyRequestToProduct(
            ProductRequestDto requestDto, Product product) {

        if (requestDto.getName() == null
                || requestDto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Məhsulun adı boş ola bilməz!");
        }
        String name = requestDto.getName().trim();
        if (name.length() > 255) {
            throw new IllegalArgumentException(
                    "Məhsulun adı maksimum 255 simvol ola bilər!");
        }

        if (requestDto.getCode() == null
                || requestDto.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Məhsulun kodu boş ola bilməz!");
        }
        String code = ProductCodeUtils.normalizeCode(requestDto.getCode());
        requestDto.setCode(code);
        if (code.length() > 255) {
            throw new IllegalArgumentException(
                    "Məhsulun kodu maksimum 255 simvol ola bilər!");
        }

        List<Product> existingProducts = productRepository.findByCodeIgnoreCase(code);
        boolean isDuplicate = false;
        if (existingProducts != null && !existingProducts.isEmpty()) {
            for (Product existing : existingProducts) {
                if (existing != null && (product.getId() == null || !existing.getId().equals(product.getId()))) {
                    isDuplicate = true;
                    break;
                }
            }
        } else {
            Product existingProduct = productRepository.findByCode(code).orElse(null);
            if (existingProduct != null && (product.getId() == null || !existingProduct.getId().equals(product.getId()))) {
                isDuplicate = true;
            }
        }

        if (isDuplicate) {
            throw new IllegalArgumentException(
                    "Bu kodla məhsul artıq mövcuddur!");
        }

        if (requestDto.getPrice() == null) {
            throw new IllegalArgumentException(
                    "Qiymət daxil edilməlidir!");
        }
        if (requestDto.getPrice().compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException(
                    "Qiymət minimum 0.01 AZN olmalıdır!");
        }
        if (requestDto.getPrice().scale() > 2) {
            throw new IllegalArgumentException(
                    "Qiymət maksimum 2 onluq rəqəmdən ibarət ola bilər!");
        }

        if (requestDto.getStockQuantity() == null) {
            throw new IllegalArgumentException(
                    "Stok miqdarı daxil edilməlidir!");
        }
        if (requestDto.getStockQuantity() < 0) {
            throw new IllegalArgumentException(
                    "Stok miqdarı mənfi ola bilməz!");
        }

        String yearRange = null;
        if (requestDto.getYearRange() != null) {
            String trimmedYear = requestDto.getYearRange().trim();
            if (!trimmedYear.isEmpty()) {
                if (!trimmedYear.matches("^\\d{4}$")) {
                    throw new IllegalArgumentException(
                            "İstehsal ili 4 rəqəmdən ibarət olmalıdır (məsələn: 2024)!");
                }
                yearRange = trimmedYear;
            }
        }

        if (requestDto.getCategoryId() == null || requestDto.getCategoryId() <= 0) {
            throw new IllegalArgumentException(
                    "Kateqoriya seçilməlidir!");
        }
        Category category = categoryRepository
                .findById(requestDto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Kateqoriya tapılmadı! ID: " + requestDto.getCategoryId()));

        CarBrand carBrand = null;
        if (requestDto.getCarBrandId() != null) {
            if (requestDto.getCarBrandId() <= 0) {
                throw new IllegalArgumentException(
                        "Düzgün avtomobil markası seçilməlidir!");
            }
            carBrand = carBrandRepository
                    .findById(requestDto.getCarBrandId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Avtomobil markası tapılmadı! ID: " + requestDto.getCarBrandId()));
        }

        String description = trimToNull(requestDto.getDescription());
        if (description != null && description.length() > 255) {
            throw new IllegalArgumentException(
                    "Məzmun maksimum 255 simvol ola bilər!");
        }

        String engine = trimToNull(requestDto.getEngine());
        if (engine != null && engine.length() > 255) {
            throw new IllegalArgumentException(
                    "Mühərrik maksimum 255 simvol ola bilər!");
        }

        String oemCode = trimToNull(requestDto.getOemCode());
        if (oemCode != null && oemCode.length() > 255) {
            throw new IllegalArgumentException(
                    "OEM kodu maksimum 255 simvol ola bilər!");
        }

        String imageUrl = trimToNull(requestDto.getImageUrl());
        if (imageUrl != null && imageUrl.length() > 255) {
            throw new IllegalArgumentException(
                    "Şəkil linki maksimum 255 simvol ola bilər!");
        }

        product.setName(name);
        product.setCode(code);
        product.setDescription(description);
        product.setPrice(requestDto.getPrice());
        product.setStockQuantity(requestDto.getStockQuantity());
        product.setYearRange(yearRange);
        product.setImageUrl(imageUrl);
        product.setEngine(engine);
        product.setOemCode(oemCode);
        product.setIsActive(
                requestDto.getIsActive() == null
                        ? true
                        : requestDto.getIsActive());
        product.setCategory(category);
        product.setCarBrand(carBrand);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProductResponseDto mapToResponseDto(Product product) {
        ProductResponseDto dto = new ProductResponseDto();

        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setCode(ProductCodeUtils.normalizeCode(product.getCode()));
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setYearRange(product.getYearRange());
        dto.setImageUrl(product.getImageUrl());
        dto.setEngine(product.getEngine());
        dto.setOemCode(product.getOemCode());
        dto.setIsActive(product.getIsActive());

        Category category = product.getCategory();

        if (category != null) {
            dto.setCategoryId(category.getId());
            dto.setCategoryName(category.getName());
        }

        CarBrand carBrand = product.getCarBrand();

        if (carBrand != null) {
            dto.setCarBrandId(carBrand.getId());
            dto.setCarBrandName(carBrand.getName());
        }

        return dto;
    }

    @Override
    public Page<ProductResponseDto> getActiveProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable)
                .map(this::mapToResponseDto);
    }

    @Override
    public ProductResponseDto getActiveProductById(Long id) {
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Məhsul tapılmadı!"));

        return mapToResponseDto(product);
    }

    @Override
    public Page<ProductResponseDto> searchActiveProducts(
            ProductFilterRequestDto filter, Pageable pageable) {
        validateFilter(filter);

        Pageable effectivePageable = pageable != null ? pageable : Pageable.unpaged();
        Specification<Product> spec = ProductSpecifications.filter(filter);

        return productRepository.findAll(spec, effectivePageable)
                .map(this::mapToResponseDto);
    }

    private void validateFilter(ProductFilterRequestDto filter) {
        if (filter == null) {
            return;
        }

        if (filter.getMinPrice() != null && filter.getMinPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum qiymət mənfi ola bilməz!");
        }

        if (filter.getMaxPrice() != null && filter.getMaxPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Maksimum qiymət mənfi ola bilməz!");
        }

        if (filter.getMinPrice() != null && filter.getMaxPrice() != null
                && filter.getMinPrice().compareTo(filter.getMaxPrice()) > 0) {
            throw new IllegalArgumentException("Minimum qiymət maksimum qiymətdən böyük ola bilməz!");
        }

        if (filter.getCategoryId() != null && filter.getCategoryId() <= 0) {
            throw new IllegalArgumentException("Kateqoriya ID-si müsbət olmalıdır!");
        }

        if (filter.getCarBrandId() != null && filter.getCarBrandId() <= 0) {
            throw new IllegalArgumentException("Avtomobil markası ID-si müsbət olmalıdır!");
        }

        if (filter.getYear() != null && (filter.getYear() < 1000 || filter.getYear() > 9999)) {
            throw new IllegalArgumentException("İl 4 rəqəmli olmalıdır!");
        }
    }

}
