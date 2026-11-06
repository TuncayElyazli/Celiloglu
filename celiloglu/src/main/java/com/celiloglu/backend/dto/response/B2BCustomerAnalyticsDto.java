package com.celiloglu.backend.dto.response;

import java.math.BigDecimal;

public class B2BCustomerAnalyticsDto {

    private Long userId;
    private String username;
    private String fullName;
    private String companyName;
    private Long orderCount;
    private BigDecimal totalOrderAmount;
    private BigDecimal totalPaidAmount;
    private BigDecimal remainingDebt;

    public B2BCustomerAnalyticsDto() {
        this.orderCount = 0L;
        this.totalOrderAmount = BigDecimal.ZERO;
        this.totalPaidAmount = BigDecimal.ZERO;
        this.remainingDebt = BigDecimal.ZERO;
    }

    public B2BCustomerAnalyticsDto(Long userId, String username, String fullName, String companyName,
                                   Long orderCount, BigDecimal totalOrderAmount,
                                   BigDecimal totalPaidAmount, BigDecimal remainingDebt) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.companyName = companyName;
        this.orderCount = orderCount != null ? orderCount : 0L;
        this.totalOrderAmount = totalOrderAmount != null ? totalOrderAmount : BigDecimal.ZERO;
        this.totalPaidAmount = totalPaidAmount != null ? totalPaidAmount : BigDecimal.ZERO;
        this.remainingDebt = remainingDebt != null ? remainingDebt : BigDecimal.ZERO;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getTotalOrderAmount() {
        return totalOrderAmount;
    }

    public void setTotalOrderAmount(BigDecimal totalOrderAmount) {
        this.totalOrderAmount = totalOrderAmount;
    }

    public BigDecimal getTotalPaidAmount() {
        return totalPaidAmount;
    }

    public void setTotalPaidAmount(BigDecimal totalPaidAmount) {
        this.totalPaidAmount = totalPaidAmount;
    }

    public BigDecimal getRemainingDebt() {
        return remainingDebt;
    }

    public void setRemainingDebt(BigDecimal remainingDebt) {
        this.remainingDebt = remainingDebt;
    }
}

