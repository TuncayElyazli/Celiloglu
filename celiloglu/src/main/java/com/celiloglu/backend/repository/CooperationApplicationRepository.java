package com.celiloglu.backend.repository;

import com.celiloglu.backend.entity.CooperationApplication;
import com.celiloglu.backend.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CooperationApplicationRepository extends JpaRepository<CooperationApplication, Long> {
    List<CooperationApplication> findByStatus(ApplicationStatus status);
}
