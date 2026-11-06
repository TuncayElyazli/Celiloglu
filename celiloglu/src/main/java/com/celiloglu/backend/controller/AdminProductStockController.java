package com.celiloglu.backend.controller;

import com.celiloglu.backend.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminProductStockController {

    private final ProductService productService;

    public AdminProductStockController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/admin/products/{id}/stock")
    public String updateStock(@PathVariable("id") Long id,
                              @RequestParam("stockQuantity") Integer stockQuantity,
                              @RequestParam(name = "expectedStock", required = false) Integer expectedStock,
                              @RequestParam(name = "redirectUrl", required = false) String redirectUrl,
                              RedirectAttributes redirectAttributes) {
        try {
            productService.updateStockQuantity(id, stockQuantity, expectedStock);
            redirectAttributes.addFlashAttribute("successMessage", "Məhsulun stoku uğurla yeniləndi.");
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (redirectUrl != null && !redirectUrl.isBlank() && redirectUrl.startsWith("/admin/")) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/admin/orders";
    }
}

