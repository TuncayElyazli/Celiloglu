package com.celiloglu.backend.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardDto {

    private long totalOrders;
    private long pendingOrders;
    private long activeProducts;
    private BigDecimal receivedPayments = BigDecimal.ZERO;
    private List<OrderResponseDto> recentOrders = new ArrayList<>();
    private List<OrderStatusStatDto> orderStatusStats = new ArrayList<>();
    private PaymentChartDto paymentChart = new PaymentChartDto();

    public AdminDashboardDto() {}

    public AdminDashboardDto(long totalOrders, long pendingOrders, long activeProducts,
                             BigDecimal receivedPayments, List<OrderResponseDto> recentOrders) {
        this(totalOrders, pendingOrders, activeProducts, receivedPayments, recentOrders, new ArrayList<>());
    }

    public AdminDashboardDto(long totalOrders, long pendingOrders, long activeProducts,
                             BigDecimal receivedPayments, List<OrderResponseDto> recentOrders,
                             List<OrderStatusStatDto> orderStatusStats) {
        this.totalOrders = totalOrders;
        this.pendingOrders = pendingOrders;
        this.activeProducts = activeProducts;
        this.receivedPayments = receivedPayments != null ? receivedPayments : BigDecimal.ZERO;
        this.recentOrders = recentOrders != null ? recentOrders : new ArrayList<>();
        this.orderStatusStats = orderStatusStats != null ? orderStatusStats : new ArrayList<>();
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getActiveProducts() {
        return activeProducts;
    }

    public void setActiveProducts(long activeProducts) {
        this.activeProducts = activeProducts;
    }

    public BigDecimal getReceivedPayments() {
        return receivedPayments;
    }

    public void setReceivedPayments(BigDecimal receivedPayments) {
        this.receivedPayments = receivedPayments != null ? receivedPayments : BigDecimal.ZERO;
    }

    public List<OrderResponseDto> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<OrderResponseDto> recentOrders) {
        this.recentOrders = recentOrders != null ? recentOrders : new ArrayList<>();
    }

    public List<OrderStatusStatDto> getOrderStatusStats() {
        return orderStatusStats;
    }

    public void setOrderStatusStats(List<OrderStatusStatDto> orderStatusStats) {
        this.orderStatusStats = orderStatusStats != null ? orderStatusStats : new ArrayList<>();
    }

    public PaymentChartDto getPaymentChart() {
        return paymentChart;
    }

    public void setPaymentChart(PaymentChartDto paymentChart) {
        this.paymentChart = paymentChart != null ? paymentChart : new PaymentChartDto();
    }
}

