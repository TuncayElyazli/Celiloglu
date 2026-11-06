package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.request.CarBrandRequestDto;
import com.celiloglu.backend.dto.response.CarBrandResponseDto;
import com.celiloglu.backend.service.CarBrandService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/car-brands")
public class AdminCarBrandController {

    private final CarBrandService carBrandService;

    public AdminCarBrandController(CarBrandService carBrandService) {
        this.carBrandService = carBrandService;
    }

    @GetMapping
    public String listCarBrands(Model model) {
        List<CarBrandResponseDto> carBrands = carBrandService.getAllCarBrands();
        model.addAttribute("carBrandList", carBrands);

        if (!model.containsAttribute("carBrandForm")) {
            model.addAttribute("carBrandForm", new CarBrandRequestDto());
        }
        if (!model.containsAttribute("editingBrandId")) {
            model.addAttribute("editingBrandId", null);
        }
        return "admin/car-brands";
    }

    @GetMapping("/{id}/edit")
    public String editCarBrand(@PathVariable("id") Long id,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            CarBrandResponseDto brand = carBrandService.getCarBrandById(id);

            CarBrandRequestDto form = new CarBrandRequestDto();
            form.setName(brand.getName());

            model.addAttribute("carBrandForm", form);
            model.addAttribute("editingBrandId", id);

            List<CarBrandResponseDto> carBrands = carBrandService.getAllCarBrands();
            model.addAttribute("carBrandList", carBrands);

            return "admin/car-brands";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("brandError", ex.getMessage());
            return "redirect:/admin/car-brands";
        }
    }

    @PostMapping
    public String createCarBrand(
            @ModelAttribute("carBrandForm") CarBrandRequestDto form,
            BindingResult bindingResult,
            Model model, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("brandError", "Daxil edilən məlumat düzgün deyil!");
            model.addAttribute("carBrandList", carBrandService.getAllCarBrands());
            model.addAttribute("editingBrandId", null);
            return "admin/car-brands";
        }

        try {
            carBrandService.createCarBrand(form);
            redirectAttributes.addFlashAttribute("brandSuccess",
                    "Avtomobil markası uğurla əlavə edildi.");
            return "redirect:/admin/car-brands";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("brandError", ex.getMessage());
            model.addAttribute("carBrandList", carBrandService.getAllCarBrands());
            model.addAttribute("editingBrandId", null);
            return "admin/car-brands";
        }
    }

    @PostMapping("/{id}")
    public String updateCarBrand(@PathVariable("id") Long id,
            @ModelAttribute("carBrandForm") CarBrandRequestDto form,
            BindingResult bindingResult,
            Model model, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("brandError", "Daxil edilən məlumat düzgün deyil!");
            model.addAttribute("editingBrandId", id);
            model.addAttribute("carBrandList", carBrandService.getAllCarBrands());
            return "admin/car-brands";
        }

        try {
            carBrandService.updateCarBrand(id, form);
            redirectAttributes.addFlashAttribute("brandSuccess",
                    "Avtomobil markası uğurla yeniləndi.");
            return "redirect:/admin/car-brands";
        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            model.addAttribute("brandError", ex.getMessage());
            model.addAttribute("editingBrandId", id);
            model.addAttribute("carBrandList", carBrandService.getAllCarBrands());
            return "admin/car-brands";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteCarBrand(@PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {
        try {
            carBrandService.deleteCarBrand(id);
            redirectAttributes.addFlashAttribute("brandSuccess",
                    "Avtomobil markası uğurla silindi.");
        } catch (EntityNotFoundException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("brandError", ex.getMessage());
        }
        return "redirect:/admin/car-brands";
    }
}
