package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.request.CartItemRequestDto;
import com.celiloglu.backend.dto.response.CartResponseDto;
import com.celiloglu.backend.security.CustomUserDetails;
import com.celiloglu.backend.service.CartService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping({ "/cart", "/cart.html" })
    public String cart(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            CartResponseDto cart = cartService.getCart(userDetails.getId());
            model.addAttribute("cart", cart);
        } catch (Exception ex) {
            model.addAttribute("errorMessage",
                    ex.getMessage() != null ? ex.getMessage() : "Səbət yüklənərkən xəta baş verdi.");
            model.addAttribute("cart", new CartResponseDto());
        }
        return "cart";
    }

    @PostMapping("/cart/items/add-by-code")
    public String addItemByCode(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
            RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        if (code == null || code.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Məhsul kodu boş ola bilməz!");
            return "redirect:/cart";
        }
        if (quantity == null || quantity < 1) {
            redirectAttributes.addFlashAttribute("errorMessage", "Məhsul sayı ən azı 1 olmalıdır!");
            return "redirect:/cart";
        }
        try {
            cartService.addItemByCode(userDetails.getId(), code.trim(), quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Məhsul səbətə əlavə edildi.");
        } catch (EntityNotFoundException | IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/add")
    public String addItem(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
            RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        if (productId == null || productId <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Məhsul ID-si müsbət ədəd olmalıdır!");
            return "redirect:/cart";
        }
        if (quantity == null || quantity < 1) {
            redirectAttributes.addFlashAttribute("errorMessage", "Məhsul sayı ən azı 1 olmalıdır!");
            return "redirect:/cart";
        }
        try {
            CartItemRequestDto request = new CartItemRequestDto(productId, quantity);
            cartService.addItemToCart(userDetails.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Məhsul səbətə əlavə edildi.");
        } catch (EntityNotFoundException | IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/update")
    public String updateQuantity(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        if (productId == null || productId <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Məhsul ID-si müsbət ədəd olmalıdır!");
            return "redirect:/cart";
        }
        if (quantity == null || quantity < 1) {
            redirectAttributes.addFlashAttribute("errorMessage", "Məhsul sayı ən azı 1 olmalıdır!");
            return "redirect:/cart";
        }
        try {
            cartService.updateItemQuantity(userDetails.getId(), productId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Məhsulun miqdarı yeniləndi.");
        } catch (EntityNotFoundException | IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/remove")
    public String removeItem(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "productId", required = false) Long productId,
            RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        if (productId == null || productId <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Məhsul ID-si müsbət ədəd olmalıdır!");
            return "redirect:/cart";
        }
        try {
            cartService.removeItemFromCart(userDetails.getId(), productId);
            redirectAttributes.addFlashAttribute("successMessage", "Məhsul səbətdən silindi.");
        } catch (EntityNotFoundException | IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(@AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            cartService.clearCart(userDetails.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Səbət uğurla təmizləndi.");
        } catch (EntityNotFoundException | IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @ExceptionHandler({ MethodArgumentTypeMismatchException.class, NumberFormatException.class })
    public String handleNumberFormatException(Exception ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Daxil edilən parametr düzgün rəqəm formatında olmalıdır!");
        return "redirect:/cart";
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class, EntityNotFoundException.class })
    public String handleServiceException(Exception ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/cart";
    }
}
