package com.celiloglu.backend.repository.specification;

import com.celiloglu.backend.dto.request.ProductFilterRequestDto;
import com.celiloglu.backend.entity.Product;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSpecificationsTest {

    @Mock
    private Root<Product> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Boolean> isActivePath;

    @Mock
    private Path<Integer> stockQuantityPath;

    @Mock
    private Predicate isActivePredicate;

    @Mock
    private Predicate stockPredicate;

    @Mock
    private Predicate combinedPredicate;

    @Test
    @DisplayName("Kataloq filtri həmişə isActive=true və stockQuantity > 0 şərtlərini tətbiq edir")
    void testFilter_AppliesIsActiveAndStockGreaterThanZero() {
        when(root.<Boolean>get("isActive")).thenReturn(isActivePath);
        when(root.<Integer>get("stockQuantity")).thenReturn(stockQuantityPath);

        when(cb.isTrue(isActivePath)).thenReturn(isActivePredicate);
        when(cb.greaterThan(stockQuantityPath, 0)).thenReturn(stockPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(combinedPredicate);

        Specification<Product> spec = ProductSpecifications.filter(null);
        Predicate result = spec.toPredicate(root, query, cb);

        verify(root).get("isActive");
        verify(cb).isTrue(isActivePath);
        verify(root).get("stockQuantity");
        verify(cb).greaterThan(stockQuantityPath, 0);
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    @DisplayName("Kataloq filtri boş filter DTO ilə də isActive=true və stockQuantity > 0 şərtlərini saxlayır")
    void testFilter_WithFilterDto_AppliesIsActiveAndStockGreaterThanZero() {
        when(root.<Boolean>get("isActive")).thenReturn(isActivePath);
        when(root.<Integer>get("stockQuantity")).thenReturn(stockQuantityPath);

        when(cb.isTrue(isActivePath)).thenReturn(isActivePredicate);
        when(cb.greaterThan(stockQuantityPath, 0)).thenReturn(stockPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(combinedPredicate);

        ProductFilterRequestDto filterDto = new ProductFilterRequestDto();
        Specification<Product> spec = ProductSpecifications.filter(filterDto);
        Predicate result = spec.toPredicate(root, query, cb);

        verify(root).get("isActive");
        verify(cb).isTrue(isActivePath);
        verify(root).get("stockQuantity");
        verify(cb).greaterThan(stockQuantityPath, 0);
    }
}

