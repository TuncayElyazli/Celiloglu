package com.celiloglu.backend.dto.request;

import com.celiloglu.backend.enums.PaymentMethod;

import java.math.BigDecimal;

public class PaymentCreateRequestDto {

    private Long orderId;
    private String receiptNumber;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String notes;

    public PaymentCreateRequestDto() {}

    public PaymentCreateRequestDto(Long orderId, String receiptNumber, BigDecimal amount, PaymentMethod paymentMethod, String notes) {
        this.orderId = orderId;
        this.receiptNumber = receiptNumber;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

