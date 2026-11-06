package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.response.ProductResponseDto;
import com.celiloglu.backend.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Controller
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products/{id}")
    public String getProductDetails(@PathVariable("id") Long id, Model model) {
        ProductResponseDto product = productService.getActiveProductById(id);
        model.addAttribute("product", product);
        return "product-details";
    }

    @ExceptionHandler({ EntityNotFoundException.class, MethodArgumentTypeMismatchException.class })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception ex, Model model) {
        model.addAttribute("errorMessage", "Axtardığınız məhsul tapılmadı və ya aktiv deyil.");
        return "error/404";
    }
}
