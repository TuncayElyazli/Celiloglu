package com.celiloglu.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentChartPointDto {

    private LocalDate date;
    private String formattedDate;
    private String fullDate;
    private BigDecimal amount = BigDecimal.ZERO;
    private String formattedAmount;
    private double x;
    private double y;

    public PaymentChartPointDto() {}

    public PaymentChartPointDto(LocalDate date, String formattedDate, String fullDate,
                               BigDecimal amount, String formattedAmount, double x, double y) {
        this.date = date;
        this.formattedDate = formattedDate;
        this.fullDate = fullDate;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.formattedAmount = formattedAmount;
        this.x = x;
        this.y = y;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }

    public String getFullDate() {
        return fullDate;
    }

    public void setFullDate(String fullDate) {
        this.fullDate = fullDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount : BigDecimal.ZERO;
    }

    public String getFormattedAmount() {
        return formattedAmount;
    }

    public void setFormattedAmount(String formattedAmount) {
        this.formattedAmount = formattedAmount;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}

