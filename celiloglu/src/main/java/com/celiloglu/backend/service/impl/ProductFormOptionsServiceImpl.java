package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.service.ProductFormOptionsService;

import com.celiloglu.backend.repository.CarBrandRepository;
import com.celiloglu.backend.repository.CategoryRepository;
import com.celiloglu.backend.repository.ProductRepository;
import com.celiloglu.backend.service.ProductFormOptionsService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductFormOptionsServiceImpl implements ProductFormOptionsService {

    private final CategoryRepository categoryRepository;
    private final CarBrandRepository carBrandRepository;
    private final ProductRepository productRepository;

    public ProductFormOptionsServiceImpl(CategoryRepository categoryRepository,
            CarBrandRepository carBrandRepository,
            ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.carBrandRepository = carBrandRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<Choice> getCategories() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(category -> new Choice(category.getId(), category.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Choice> getCarBrands() {
        return carBrandRepository.findAll(Sort.by("name")).stream()
                .map(brand -> new Choice(brand.getId(), brand.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> getYears() {
        List<String> rawRanges = productRepository.findDistinctActiveYearRanges();
        Set<Integer> years = new HashSet<>();

        for (String raw : rawRanges) {
            if (raw == null) {
                continue;
            }
            String normalized = raw.replace(" ", "").replace("–", "-").replace("—", "-").trim();
            if (normalized.matches("^\\d{4}$")) {
                try {
                    years.add(Integer.parseInt(normalized));
                } catch (NumberFormatException ignored) {
                }
            } else if (normalized.matches("^\\d{4}-\\d{4}$")) {
                try {
                    int start = Integer.parseInt(normalized.substring(0, 4));
                    int end = Integer.parseInt(normalized.substring(5, 9));
                    if (start <= end && end - start <= 50) {
                        for (int y = start; y <= end; y++) {
                            years.add(y);
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return years.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

}
