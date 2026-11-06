package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.request.PaymentCreateRequestDto;
import com.celiloglu.backend.dto.response.OrderItemResponseDto;
import com.celiloglu.backend.dto.response.OrderResponseDto;
import com.celiloglu.backend.dto.response.PaymentResponseDto;
import com.celiloglu.backend.enums.PaymentMethod;
import com.celiloglu.backend.security.CustomUserDetails;
import com.celiloglu.backend.service.OrderExcelExportService;
import com.celiloglu.backend.service.OrderService;
import com.celiloglu.backend.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class AdminOrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final OrderExcelExportService orderExcelExportService;

    @Autowired
    public AdminOrderController(OrderService orderService,
                                PaymentService paymentService,
                                OrderExcelExportService orderExcelExportService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.orderExcelExportService = orderExcelExportService;
    }

    @GetMapping({ "/admin/orders", "/admin/orders.html" })
    public String listOrders(@RequestParam(name = "page", defaultValue = "0") int page,
                             @RequestParam(name = "size", defaultValue = "10") int size,
                             Model model) {
        int pageNumber = Math.max(page, 0);
        int pageSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("id")));

        Page<OrderResponseDto> ordersPage = orderService.getAllOrdersForAdmin(pageable);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isWarehouse = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_WAREHOUSE_MANAGER".equals(a.getAuthority()));
        if (isWarehouse && ordersPage != null && ordersPage.getContent() != null) {
            for (OrderResponseDto o : ordersPage.getContent()) {
                o.setTotalAmount(null);
            }
        }

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("activePage", "orders");
        return "admin/orders";
    }

    @GetMapping("/admin/orders/export/excel")
    public ResponseEntity<byte[]> exportOrdersToExcel() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean hasRole = auth.getAuthorities().stream().anyMatch(a -> {
            String r = a.getAuthority();
            return "ROLE_ADMIN".equals(r) || "ROLE_SUPER_ADMIN".equals(r) || "ROLE_CASHIER".equals(r) || "ROLE_WAREHOUSE_MANAGER".equals(r);
        });
        if (!hasRole) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean includeFinancials = auth.getAuthorities().stream().anyMatch(a -> {
            String r = a.getAuthority();
            return "ROLE_ADMIN".equals(r) || "ROLE_SUPER_ADMIN".equals(r) || "ROLE_CASHIER".equals(r);
        });

        byte[] excelData = orderExcelExportService.exportOrdersToExcel(includeFinancials);
        String filename = "sifarisler_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @GetMapping("/admin/orders/{id}")
    public String getOrderDetails(@PathVariable("id") Long id, Model model) {
        if (id == null || id <= 0) {
            throw new EntityNotFoundException("Sifariş tapılmadı!");
        }

        OrderResponseDto order = orderService.getOrderDetailsForAdmin(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isWarehouse = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_WAREHOUSE_MANAGER".equals(a.getAuthority()));

        if (isWarehouse) {
            order.setPayments(java.util.Collections.emptyList());
            order.setPaidAmount(BigDecimal.ZERO);
            order.setRemainingAmount(BigDecimal.ZERO);
            order.setTotalAmount(null);
            if (order.getItems() != null) {
                for (OrderItemResponseDto item : order.getItems()) {
                    item.setPriceAtPurchase(null);
                    item.setSubtotal(null);
                }
            }
            model.addAttribute("order", order);
            model.addAttribute("payments", java.util.Collections.emptyList());
            model.addAttribute("paidAmount", BigDecimal.ZERO);
            model.addAttribute("remainingAmount", BigDecimal.ZERO);
            model.addAttribute("activePage", "orders");
            return "admin/order-details";
        }

        List<PaymentResponseDto> payments = paymentService.getPaymentsByOrderId(id);
        BigDecimal paidAmount = paymentService.getPaidAmountByOrderId(id);
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal remainingAmount = totalAmount.subtract(paidAmount);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        order.setPayments(payments);
        order.setPaidAmount(paidAmount);
        order.setRemainingAmount(remainingAmount);

        model.addAttribute("order", order);
        model.addAttribute("payments", payments);
        model.addAttribute("paidAmount", paidAmount);
        model.addAttribute("remainingAmount", remainingAmount);
        model.addAttribute("activePage", "orders");

        if (!model.containsAttribute("receiptNumber")) {
            model.addAttribute("receiptNumber", paymentService.generateReceiptNumber());
        }

        return "admin/order-details";
    }

    @PostMapping("/admin/orders/{id}/status")
    public String updateOrderStatus(@PathVariable("id") Long id,
                                    @RequestParam(name = "targetStatus", required = false) String targetStatus,
                                    RedirectAttributes redirectAttributes) {
        if (id == null || id <= 0) {
            throw new EntityNotFoundException("Sifariş tapılmadı!");
        }

        try {
            orderService.updateOrderStatusForAdmin(id, targetStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Sifarişin statusu uğurla yeniləndi.");
            return "redirect:/admin/orders/" + id;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/orders/" + id;
        }
    }

    @PostMapping("/admin/orders/{id}/payments")
    public String recordPayment(@PathVariable("id") Long id,
                                @RequestParam(name = "receiptNumber", required = false) String receiptNumber,
                                @RequestParam(name = "amount", required = false) String amountStr,
                                @RequestParam(name = "paymentMethod", required = false) String paymentMethodStr,
                                @RequestParam(name = "notes", required = false) String notes,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        if (id == null || id <= 0) {
            throw new EntityNotFoundException("Sifariş tapılmadı!");
        }

        // Tapılmayan sifariş üçün həqiqi 404 qaytarır
        orderService.getOrderDetailsForAdmin(id);

        String effectiveReceiptNumber = (receiptNumber != null && !receiptNumber.trim().isEmpty())
                ? receiptNumber.trim()
                : paymentService.generateReceiptNumber();

        // Validasiya xətasında daxil edilmiş dəyərlər və eyni receiptNumber saxlanılsın
        redirectAttributes.addFlashAttribute("receiptNumber", effectiveReceiptNumber);
        redirectAttributes.addFlashAttribute("enteredAmount", amountStr);
        redirectAttributes.addFlashAttribute("enteredPaymentMethod", paymentMethodStr);
        redirectAttributes.addFlashAttribute("enteredNotes", notes);

        BigDecimal amount;
        if (amountStr == null || amountStr.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ödəniş məbləği qeyd edilməlidir!");
            return "redirect:/admin/orders/" + id;
        }
        try {
            amount = new BigDecimal(amountStr.trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Düzgün məbləğ formatı daxil edin!");
            return "redirect:/admin/orders/" + id;
        }

        PaymentMethod paymentMethod;
        if (paymentMethodStr == null || paymentMethodStr.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ödəniş üsulu seçilməlidir!");
            return "redirect:/admin/orders/" + id;
        }
        try {
            paymentMethod = PaymentMethod.valueOf(paymentMethodStr.trim());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Yolverilməz ödəniş üsulu!");
            return "redirect:/admin/orders/" + id;
        }

        String recordedBy = userDetails != null ? userDetails.getUsername() : "ADMIN";

        PaymentCreateRequestDto request = new PaymentCreateRequestDto(
                id, effectiveReceiptNumber, amount, paymentMethod, notes
        );

        try {
            PaymentResponseDto payment = paymentService.recordPayment(request, recordedBy);
            // Uğurlu olduqda daxil edilmiş müvəqqəti dəyərləri təmizlə
            redirectAttributes.getFlashAttributes().remove("receiptNumber");
            redirectAttributes.getFlashAttributes().remove("enteredAmount");
            redirectAttributes.getFlashAttributes().remove("enteredPaymentMethod");
            redirectAttributes.getFlashAttributes().remove("enteredNotes");
            redirectAttributes.addFlashAttribute("successMessage",
                    "Ödəniş uğurla qeydə alındı. Qəbz №: " + payment.getReceiptNumber());
            return "redirect:/admin/orders/" + id;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/orders/" + id;
        }
    }

    @ExceptionHandler({ EntityNotFoundException.class, MethodArgumentTypeMismatchException.class })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception ex, Model model) {
        model.addAttribute("errorMessage", "Sifariş tapılmadı!");
        return "error/404";
    }
}
