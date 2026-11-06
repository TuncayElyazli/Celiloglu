package com.celiloglu.backend.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PaymentChartDto {

    private boolean hasPayments;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal maxAmount = BigDecimal.ZERO;
    private List<PaymentChartPointDto> points = new ArrayList<>();
    private List<PaymentChartAxisLabelDto> yAxisLabels = new ArrayList<>();
    private List<PaymentChartAxisLabelDto> xAxisLabels = new ArrayList<>();
    private String svgPathLine = "";
    private String svgPathArea = "";

    public PaymentChartDto() {}

    public boolean isHasPayments() {
        return hasPayments;
    }

    public void setHasPayments(boolean hasPayments) {
        this.hasPayments = hasPayments;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount != null ? maxAmount : BigDecimal.ZERO;
    }

    public List<PaymentChartPointDto> getPoints() {
        return points;
    }

    public void setPoints(List<PaymentChartPointDto> points) {
        this.points = points != null ? points : new ArrayList<>();
    }

    public List<PaymentChartAxisLabelDto> getYAxisLabels() {
        return yAxisLabels;
    }

    public List<PaymentChartAxisLabelDto> getyAxisLabels() {
        return yAxisLabels;
    }

    public void setYAxisLabels(List<PaymentChartAxisLabelDto> yAxisLabels) {
        this.yAxisLabels = yAxisLabels != null ? yAxisLabels : new ArrayList<>();
    }

    public void setyAxisLabels(List<PaymentChartAxisLabelDto> yAxisLabels) {
        this.yAxisLabels = yAxisLabels != null ? yAxisLabels : new ArrayList<>();
    }

    public List<PaymentChartAxisLabelDto> getXAxisLabels() {
        return xAxisLabels;
    }

    public List<PaymentChartAxisLabelDto> getxAxisLabels() {
        return xAxisLabels;
    }

    public void setXAxisLabels(List<PaymentChartAxisLabelDto> xAxisLabels) {
        this.xAxisLabels = xAxisLabels != null ? xAxisLabels : new ArrayList<>();
    }

    public void setxAxisLabels(List<PaymentChartAxisLabelDto> xAxisLabels) {
        this.xAxisLabels = xAxisLabels != null ? xAxisLabels : new ArrayList<>();
    }

    public String getSvgPathLine() {
        return svgPathLine;
    }

    public void setSvgPathLine(String svgPathLine) {
        this.svgPathLine = svgPathLine != null ? svgPathLine : "";
    }

    public String getSvgPathArea() {
        return svgPathArea;
    }

    public void setSvgPathArea(String svgPathArea) {
        this.svgPathArea = svgPathArea != null ? svgPathArea : "";
    }
}
