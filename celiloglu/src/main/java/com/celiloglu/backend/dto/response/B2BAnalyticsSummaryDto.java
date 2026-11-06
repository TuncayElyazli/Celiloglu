package com.celiloglu.backend.dto.response;

import java.math.BigDecimal;

public class B2BAnalyticsSummaryDto {

    private BigDecimal totalOrderAmount;
    private BigDecimal totalPaidAmount;
    private BigDecimal totalRemainingDebt;

    public B2BAnalyticsSummaryDto() {
        this.totalOrderAmount = BigDecimal.ZERO;
        this.totalPaidAmount = BigDecimal.ZERO;
        this.totalRemainingDebt = BigDecimal.ZERO;
    }

    public B2BAnalyticsSummaryDto(BigDecimal totalOrderAmount, BigDecimal totalPaidAmount, BigDecimal totalRemainingDebt) {
        this.totalOrderAmount = totalOrderAmount != null ? totalOrderAmount : BigDecimal.ZERO;
        this.totalPaidAmount = totalPaidAmount != null ? totalPaidAmount : BigDecimal.ZERO;
        this.totalRemainingDebt = totalRemainingDebt != null ? totalRemainingDebt : BigDecimal.ZERO;
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

    public BigDecimal getTotalRemainingDebt() {
        return totalRemainingDebt;
    }

    public void setTotalRemainingDebt(BigDecimal totalRemainingDebt) {
        this.totalRemainingDebt = totalRemainingDebt;
    }
}

