package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.dto.response.B2BAnalyticsSummaryDto;
import com.celiloglu.backend.dto.response.B2BCustomerAnalyticsDto;
import com.celiloglu.backend.enums.OrderStatus;
import com.celiloglu.backend.enums.PaymentStatus;
import com.celiloglu.backend.repository.OrderRepository;
import com.celiloglu.backend.service.AdminAnalyticsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final OrderRepository orderRepository;

    public AdminAnalyticsServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public B2BAnalyticsSummaryDto getAnalyticsSummary() {
        List<Object[]> rows = orderRepository.getB2BAnalyticsSummary(
                OrderStatus.LƏĞV_EDİLDİ.name(),
                PaymentStatus.TAM_ÖDƏNİLMİŞ.name()
        );

        if (rows == null || rows.isEmpty() || rows.get(0) == null) {
            return new B2BAnalyticsSummaryDto(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            );
        }

        Object[] row = rows.get(0);
        BigDecimal totalOrderAmount = toBigDecimal(row[0]);
        BigDecimal totalPaidAmount = toBigDecimal(row[1]);
        BigDecimal totalRemainingDebt = toBigDecimal(row[2]);

        return new B2BAnalyticsSummaryDto(totalOrderAmount, totalPaidAmount, totalRemainingDebt);
    }

    @Override
    public Page<B2BCustomerAnalyticsDto> getCustomerAnalytics(int page, int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = (size > 0 && size <= 100) ? size : 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Object[]> pageResult = orderRepository.findB2BCustomerAnalytics(
                OrderStatus.LƏĞV_EDİLDİ.name(),
                PaymentStatus.TAM_ÖDƏNİLMİŞ.name(),
                pageable
        );

        List<B2BCustomerAnalyticsDto> dtoList = new ArrayList<>();
        for (Object[] row : pageResult.getContent()) {
            dtoList.add(mapToCustomerAnalyticsDto(row));
        }

        return new PageImpl<>(dtoList, pageable, pageResult.getTotalElements());
    }

    @Override
    public List<B2BCustomerAnalyticsDto> getAllCustomerAnalytics() {
        List<Object[]> rows = orderRepository.findAllB2BCustomerAnalytics(
                OrderStatus.LƏĞV_EDİLDİ.name(),
                PaymentStatus.TAM_ÖDƏNİLMİŞ.name()
        );

        List<B2BCustomerAnalyticsDto> dtoList = new ArrayList<>();
        if (rows == null) {
            return dtoList;
        }

        for (Object[] row : rows) {
            dtoList.add(mapToCustomerAnalyticsDto(row));
        }

        return dtoList;
    }

    private B2BCustomerAnalyticsDto mapToCustomerAnalyticsDto(Object[] row) {
        Long userId = toLong(row[0]);
        String username = row[1] != null ? row[1].toString() : null;
        String firstName = row[2] != null ? row[2].toString() : null;
        String lastName = row[3] != null ? row[3].toString() : null;
        String rawCompany = row[4] != null ? row[4].toString() : null;
        Long orderCount = toLong(row[5]);
        BigDecimal totalOrderAmount = toBigDecimal(row[6]);
        BigDecimal totalPaidAmount = toBigDecimal(row[7]);
        BigDecimal remainingDebt = toBigDecimal(row[8]);

        String fullName = null;
        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            fullName = firstName.trim() + " " + lastName.trim();
        } else if (firstName != null && !firstName.isBlank()) {
            fullName = firstName.trim();
        } else if (lastName != null && !lastName.isBlank()) {
            fullName = lastName.trim();
        }

        if (fullName == null || fullName.isBlank()) {
            fullName = (username != null && !username.isBlank()) ? username : "—";
        }

        String companyName = (rawCompany != null && !rawCompany.isBlank()) ? rawCompany.trim() : "—";

        return new B2BCustomerAnalyticsDto(
                userId,
                username,
                fullName,
                companyName,
                orderCount,
                totalOrderAmount,
                totalPaidAmount,
                remainingDebt
        );
    }

    private Long toLong(Object val) {
        if (val == null) {
            return 0L;
        }
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        try {
            return Long.parseLong(val.toString().trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (val instanceof BigDecimal) {
            return ((BigDecimal) val).setScale(2, RoundingMode.HALF_UP);
        }
        if (val instanceof Number) {
            return BigDecimal.valueOf(((Number) val).doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(val.toString().trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
