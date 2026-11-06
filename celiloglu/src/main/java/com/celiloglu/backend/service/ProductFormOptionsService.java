package com.celiloglu.backend.service;

import java.util.List;

public interface ProductFormOptionsService {

    List<Choice> getCategories();

    List<Choice> getCarBrands();

    List<Integer> getYears();

    // Formadakı seçimlər üçün yalnız ID və ad lazımdır.
    public static class Choice {
        private final Long id;
        private final String name;

        public Choice(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
