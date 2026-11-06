package com.celiloglu.backend.repository;

import com.celiloglu.backend.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<Faq> findByCategory(String category);
}
