package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.response.OrderResponseDto;
import com.celiloglu.backend.dto.response.PaymentResponseDto;
import com.celiloglu.backend.security.CustomUserDetails;
import com.celiloglu.backend.service.OrderService;
import com.celiloglu.backend.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import java.util.Collections;
import java.util.List;

@Controller
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;
    private final PaymentService paymentService;

    @Autowired
    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    public OrderController(OrderService orderService) {
        this(orderService, null);
    }

    @GetMapping({ "/orders", "/orders.html" })
    public String orders(@AuthenticationPrincipal CustomUserDetails userDetails,
                         @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable,
                         Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Page<OrderResponseDto> ordersPage = orderService.getUserOrders(userDetails.getId(), pageable);
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("orders", ordersPage.getContent());
        return "orders";
    }

    @PostMapping("/orders")
    public String createOrder(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam(value = "shippingAddress", required = false) String shippingAddress,
                              @RequestParam(value = "orderNotes", required = false) String orderNotes,
                              RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            OrderResponseDto orderResponse = orderService.createOrderFromCart(
                    userDetails.getId(), shippingAddress, orderNotes
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Sifarişiniz uğurla rəsmiləşdirildi! Sifariş №: #" + orderResponse.getOrderNumber()
            );
            return "redirect:/orders";
        } catch (IllegalArgumentException | IllegalStateException | EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("shippingAddress", shippingAddress);
            redirectAttributes.addFlashAttribute("orderNotes", orderNotes);
            return "redirect:/cart";
        } catch (Exception ex) {
            log.error("Sifariş yaradılarkən gözlənilməz xəta baş verdi", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Sifariş yaradılarkən xəta baş verdi. Zəhmət olmasa yenidən cəhd edin.");
            redirectAttributes.addFlashAttribute("shippingAddress", shippingAddress);
            redirectAttributes.addFlashAttribute("orderNotes", orderNotes);
            return "redirect:/cart";
        }
    }

    @GetMapping("/orders/{id}")
    public String getOrderDetails(@PathVariable("id") Long id,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        if (id == null || id <= 0) {
            throw new EntityNotFoundException("Sifariş tapılmadı!");
        }

        // 1. userId yalnız CustomUserDetails.getId()-dən götürülür.
        // Əvvəl mövcud sahiblik yoxlaması ilə sifarişin cari istifadəçiyə aid olduğunu təsdiqlə:
        OrderResponseDto order = orderService.getOrderDetails(userDetails.getId(), id);

        // 2. Sahibliyi təsdiqlənmiş sifarişin ödənişlərini və maliyyə vəziyyətini oxu:
        List<PaymentResponseDto> payments = (paymentService != null)
                ? paymentService.getCustomerPaymentsByOrderId(id)
                : Collections.emptyList();

        BigDecimal paidAmount = (paymentService != null)
                ? paymentService.getPaidAmountByOrderId(id)
                : BigDecimal.ZERO;

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

        return "order-details";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable("id") Long id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        if (id == null || id <= 0) {
            throw new EntityNotFoundException("Sifariş tapılmadı!");
        }

        try {
            orderService.cancelOrder(userDetails.getId(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Sifariş ləğv edildi.");
            return "redirect:/orders";
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/orders";
        }
    }

    @ExceptionHandler({ EntityNotFoundException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception ex, Model model) {
        model.addAttribute("errorMessage", "Sifariş tapılmadı!");
        return "error/404";
    }
}

