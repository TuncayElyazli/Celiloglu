package com.celiloglu.backend.repository;

import com.celiloglu.backend.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByCode(String code);

    List<Product> findByCodeIgnoreCase(String code);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByCarBrandId(Long carBrandId);

    List<Product> findByNameContainingIgnoreCase(String name);

    // Müştəri üçün aktiv məhsulların səhifələnmiş siyahısı
    Page<Product> findByIsActiveTrue(Pageable pageable);

    // Bütün aktiv məhsulların sayı (stokundan asılı olmayaraq)
    long countByIsActiveTrue();

    // Müştəri üçün bir aktiv məhsulun tapılması
    Optional<Product> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT DISTINCT p.yearRange FROM Product p WHERE p.isActive = true AND p.yearRange IS NOT NULL")
    List<String> findDistinctActiveYearRanges();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
}