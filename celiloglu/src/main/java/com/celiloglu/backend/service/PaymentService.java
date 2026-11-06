package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.request.PaymentCreateRequestDto;
import com.celiloglu.backend.dto.response.PaymentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PaymentService {

    PaymentResponseDto recordPayment(PaymentCreateRequestDto request, String recordedBy);

    List<PaymentResponseDto> getPaymentsByOrderId(Long orderId);

    List<PaymentResponseDto> getCustomerPaymentsByOrderId(Long orderId);

    BigDecimal getPaidAmountByOrderId(Long orderId);

    Page<PaymentResponseDto> getAllPaymentsForAdmin(Pageable pageable);

    Page<PaymentResponseDto> getPaymentsForAdmin(LocalDate startDate, LocalDate endDate, Pageable pageable);

    String generateReceiptNumber();
}

