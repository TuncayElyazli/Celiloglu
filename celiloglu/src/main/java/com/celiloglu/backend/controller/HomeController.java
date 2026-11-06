package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.request.ProductFilterRequestDto;
import com.celiloglu.backend.dto.response.ProductResponseDto;
import com.celiloglu.backend.service.ProductFormOptionsService;
import com.celiloglu.backend.service.ProductService;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@Controller
public class HomeController {

    private final ProductService productService;
    private final ProductFormOptionsService optionsService;

    public HomeController(ProductService productService, ProductFormOptionsService optionsService) {
        this.productService = productService;
        this.optionsService = optionsService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping({ "/", "/index", "/index.html" })
    public String index(
            @ModelAttribute("filter") ProductFilterRequestDto filter,
            BindingResult bindingResult,
            Model model,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        if (!isAuthenticated()) {
            return "redirect:/login";
        }

        List<ProductFormOptionsService.Choice> categories = optionsService.getCategories();
        List<ProductFormOptionsService.Choice> carBrands = optionsService.getCarBrands();
        List<Integer> years = optionsService.getYears();

        model.addAttribute("categories", categories);
        model.addAttribute("carBrands", carBrands);
        model.addAttribute("years", years);

        String selectedBrandName = null;
        if (filter != null && filter.getCarBrandId() != null) {
            selectedBrandName = carBrands.stream()
                    .filter(b -> b.getId().equals(filter.getCarBrandId()))
                    .map(ProductFormOptionsService.Choice::getName)
                    .findFirst()
                    .orElse(null);
        }
        model.addAttribute("selectedBrandName", selectedBrandName);

        String selectedCategoryName = null;
        if (filter != null && filter.getCategoryId() != null) {
            selectedCategoryName = categories.stream()
                    .filter(c -> c.getId().equals(filter.getCategoryId()))
                    .map(ProductFormOptionsService.Choice::getName)
                    .findFirst()
                    .orElse(null);
        }
        model.addAttribute("selectedCategoryName", selectedCategoryName);

        Page<ProductResponseDto> productPage;

        if (bindingResult.hasErrors()) {
            model.addAttribute("filterError", getBindingErrorMessage(bindingResult));
            productPage = Page.empty(pageable);
        } else {
            try {
                productPage = productService.searchActiveProducts(filter, pageable);
            } catch (IllegalArgumentException ex) {
                model.addAttribute("filterError", ex.getMessage());
                productPage = Page.empty(pageable);
            }
        }

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("productCount", productPage.getTotalElements());

        return "index";
    }

    private String getBindingErrorMessage(BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors("minPrice") || bindingResult.hasFieldErrors("maxPrice")) {
            return "Qiymət sahələrinə yalnız düzgün rəqəm daxil edilməlidir!";
        }
        if (bindingResult.hasFieldErrors("year")) {
            return "İl sahəsinə yalnız düzgün rəqəm (məs: 2020) daxil edilməlidir!";
        }
        if (bindingResult.hasFieldErrors("carBrandId") || bindingResult.hasFieldErrors("categoryId")) {
            return "Seçilmiş kateqoriya və ya marka identifikatoru düzgün deyil!";
        }
        return "Daxil edilən filtr parametrlərində xəta var!";
    }

    @GetMapping({ "/faq", "/faq.html" })
    public String faq() {
        return "faq";
    }

    @GetMapping({ "/terms", "/terms.html" })
    public String terms() {
        return "terms";
    }

    @GetMapping({ "/privacy", "/privacy.html" })
    public String privacy() {
        return "privacy";
    }

    @GetMapping({ "/cooperation", "/cooperation.html" })
    public String cooperation() {
        return "cooperation";
    }

    @GetMapping({ "/about", "/about.html" })
    public String about() {
        return "about";
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
