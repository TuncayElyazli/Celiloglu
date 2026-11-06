package com.celiloglu.backend.repository.specification;

import com.celiloglu.backend.dto.request.ProductFilterRequestDto;
import com.celiloglu.backend.entity.Product;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> filter(ProductFilterRequestDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Həmişə yalnız isActive=true və stockQuantity > 0 məhsullar gəlsin.
            predicates.add(cb.isTrue(root.get("isActive")));
            predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));

            if (filter == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // Kod üzrə trim edilmiş, böyük/kiçik hərf fərqi olmayan hissəvi axtarış.
            // İstifadəçinin % və _ simvollarını literal qəbul et.
            if (filter.getCode() != null) {
                String trimmedCode = filter.getCode().trim();
                if (!trimmedCode.isEmpty()) {
                    String escapedCode = trimmedCode
                            .replace("\\", "\\\\")
                            .replace("%", "\\%")
                            .replace("_", "\\_");
                    String pattern = "%" + escapedCode.toLowerCase(Locale.ROOT) + "%";
                    predicates.add(cb.like(cb.lower(root.get("code")), pattern, '\\'));
                }
            }

            // Kateqoriya ID ilə dəqiq uyğunlaşsın.
            if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
            }

            // Marka ID ilə dəqiq uyğunlaşsın.
            if (filter.getCarBrandId() != null) {
                predicates.add(cb.equal(root.get("carBrand").get("id"), filter.getCarBrandId()));
            }

            // Qiymət sərhədləri daxil olmaqla yoxlanılsın.
            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }

            // İl qaydası:
            // year=2018 həm "2018", həm "2015-2020" məhsuluna uyğun gəlsin.
            // yearRange-də boşluqları və –/— tirelərini sorğu daxilində normallaşdır;
            // yalnız YYYY və YYYY-YYYY formatlarını qəbul et.
            if (filter.getYear() != null) {
                Expression<String> noSpaces = cb.function("replace", String.class,
                        root.get("yearRange"), cb.literal(" "), cb.literal(""));
                Expression<String> noEnDash = cb.function("replace", String.class,
                        noSpaces, cb.literal("–"), cb.literal("-"));
                Expression<String> normalized = cb.function("replace", String.class,
                        noEnDash, cb.literal("—"), cb.literal("-"));

                Expression<Boolean> isPattern = cb.function("regexp_like", Boolean.class,
                        normalized, cb.literal("^[0-9]{4}(-[0-9]{4})?$"));

                Expression<String> startYearStr = cb.substring(normalized, 1, 4);
                Expression<String> endYearStr = cb.function("right", String.class, normalized, cb.literal(4));

                String yearStr = String.format("%04d", filter.getYear());

                Predicate yearPredicate = cb.and(
                        cb.isNotNull(root.get("yearRange")),
                        cb.isTrue(isPattern),
                        cb.lessThanOrEqualTo(startYearStr, endYearStr),
                        cb.lessThanOrEqualTo(startYearStr, yearStr),
                        cb.greaterThanOrEqualTo(endYearStr, yearStr));
                predicates.add(yearPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
