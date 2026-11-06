package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.response.PaymentResponseDto;
import com.celiloglu.backend.service.PaymentExcelExportService;
import com.celiloglu.backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final PaymentExcelExportService paymentExcelExportService;

    @Autowired
    public AdminPaymentController(PaymentService paymentService, PaymentExcelExportService paymentExcelExportService) {
        this.paymentService = paymentService;
        this.paymentExcelExportService = paymentExcelExportService;
    }

    @GetMapping({ "/admin/payments", "/admin/payments.html" })
    public String listPayments(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        int pageNumber = Math.max(page, 0);
        int pageSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id")));

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("activePage", "payments");

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            model.addAttribute("dateFilterError", "Başlanğıc tarixi bitiş tarixindən sonra ola bilməz.");
            Page<PaymentResponseDto> emptyPage = Page.empty(pageable);
            model.addAttribute("paymentsPage", emptyPage);
            model.addAttribute("payments", emptyPage.getContent());
            return "admin/payments";
        }

        Page<PaymentResponseDto> paymentsPage = paymentService.getPaymentsForAdmin(startDate, endDate, pageable);
        model.addAttribute("paymentsPage", paymentsPage);
        model.addAttribute("payments", paymentsPage.getContent());
        return "admin/payments";
    }

    @GetMapping("/admin/payments/export/excel")
    public ResponseEntity<byte[]> exportPaymentsToExcel(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean hasAllowedRole = auth != null && auth.getAuthorities().stream().anyMatch(a -> {
            String role = a.getAuthority();
            return "ROLE_ADMIN".equals(role) || "ROLE_SUPER_ADMIN".equals(role) || "ROLE_CASHIER".equals(role);
        });
        if (!hasAllowedRole) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }

        byte[] excelData = paymentExcelExportService.exportPaymentsToExcel(startDate, endDate);
        String filename = "odenisler_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }
}
