package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.dto.request.CarBrandRequestDto;
import com.celiloglu.backend.dto.response.CarBrandResponseDto;
import com.celiloglu.backend.entity.CarBrand;
import com.celiloglu.backend.entity.Product;
import com.celiloglu.backend.repository.CarBrandRepository;
import com.celiloglu.backend.repository.ProductRepository;
import com.celiloglu.backend.service.CarBrandService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CarBrandServiceImpl implements CarBrandService {

    private final CarBrandRepository carBrandRepository;
    private final ProductRepository productRepository;

    public CarBrandServiceImpl(
            CarBrandRepository carBrandRepository,
            ProductRepository productRepository) {

        this.carBrandRepository = carBrandRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<CarBrandResponseDto> getAllCarBrands() {
        return carBrandRepository.findAll().stream()
                .map(this::mapToDto)
                .sorted(Comparator.comparing(CarBrandResponseDto::getName,
                        String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    public CarBrandResponseDto getCarBrandById(Long id) {
        CarBrand carBrand = findCarBrand(id);
        return mapToDto(carBrand);
    }

    @Override
    @Transactional
    public CarBrandResponseDto createCarBrand(CarBrandRequestDto requestDto) {
        validateAndCheckDuplicate(requestDto, null);

        CarBrand carBrand = new CarBrand();
        carBrand.setName(requestDto.getName().trim());

        CarBrand savedCarBrand = carBrandRepository.save(carBrand);
        return mapToDto(savedCarBrand);
    }

    @Override
    @Transactional
    public CarBrandResponseDto updateCarBrand(Long id, CarBrandRequestDto requestDto) {
        CarBrand carBrand = findCarBrand(id);

        validateAndCheckDuplicate(requestDto, id);

        carBrand.setName(requestDto.getName().trim());

        CarBrand updatedCarBrand = carBrandRepository.save(carBrand);
        return mapToDto(updatedCarBrand);
    }

    @Override
    @Transactional
    public void deleteCarBrand(Long id) {
        CarBrand carBrand = findCarBrand(id);

        List<Product> products = productRepository.findByCarBrandId(id);

        if (!products.isEmpty()) {
            throw new IllegalStateException(
                    "Bu markaya aid məhsullar mövcuddur, "
                            + "ona görə silinə bilməz!");
        }

        carBrandRepository.delete(carBrand);
    }

    private CarBrand findCarBrand(Long id) {
        return carBrandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Avtomobil markası tapılmadı! ID: " + id));
    }

    private void validateAndCheckDuplicate(
            CarBrandRequestDto requestDto, Long currentId) {

        if (requestDto == null
                || requestDto.getName() == null
                || requestDto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Avtomobil markasının adı boş ola bilməz!");
        }

        String trimmedName = requestDto.getName().trim();

        CarBrand existing = carBrandRepository.findByName(trimmedName)
                .orElse(null);

        // Redaktə zamanı markanın öz adına icazə verilir.
        if (existing != null
                && !existing.getId().equals(currentId)) {
            throw new IllegalArgumentException(
                    "Bu adla avtomobil markası artıq mövcuddur!");
        }
    }

    private CarBrandResponseDto mapToDto(CarBrand carBrand) {
        int productCount = productRepository
                .findByCarBrandId(carBrand.getId()).size();

        return new CarBrandResponseDto(
                carBrand.getId(),
                carBrand.getName(),
                productCount);
    }
}
