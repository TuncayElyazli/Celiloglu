package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.request.CarBrandRequestDto;
import com.celiloglu.backend.dto.response.CarBrandResponseDto;

import java.util.List;

public interface CarBrandService {

    List<CarBrandResponseDto> getAllCarBrands();

    CarBrandResponseDto getCarBrandById(Long id);

    CarBrandResponseDto createCarBrand(CarBrandRequestDto requestDto);

    CarBrandResponseDto updateCarBrand(Long id, CarBrandRequestDto requestDto);

    void deleteCarBrand(Long id);
}
