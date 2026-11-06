package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.request.CategoryRequestDto;
import com.celiloglu.backend.dto.response.CategoryDto;
import com.celiloglu.backend.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String listCategories(Model model) {
        List<CategoryDto> categories = categoryService.getAllCategories();
        model.addAttribute("categoryList", categories);

        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute("categoryForm", new CategoryRequestDto());
        }
        if (!model.containsAttribute("editingCategoryId")) {
            model.addAttribute("editingCategoryId", null);
        }
        return "admin/categories";
    }

    @GetMapping("/{id}/edit")
    public String editCategory(@PathVariable("id") Long id,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            CategoryDto category = categoryService.getCategoryById(id);

            CategoryRequestDto form = new CategoryRequestDto();
            form.setName(category.getName());
            form.setDescription(category.getDescription());

            model.addAttribute("categoryForm", form);
            model.addAttribute("editingCategoryId", id);

            List<CategoryDto> categories = categoryService.getAllCategories();
            model.addAttribute("categoryList", categories);

            return "admin/categories";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("categoryError", ex.getMessage());
            return "redirect:/admin/categories";
        }
    }

    @PostMapping
    public String createCategory(
            @ModelAttribute("categoryForm") CategoryRequestDto form,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            categoryService.createCategory(form);
            redirectAttributes.addFlashAttribute("categorySuccess",
                    "Kateqoriya uğurla əlavə edildi.");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("categoryError", ex.getMessage());
            List<CategoryDto> categories = categoryService.getAllCategories();
            model.addAttribute("categoryList", categories);
            model.addAttribute("editingCategoryId", null);
            return "admin/categories";
        }
    }

    @PostMapping("/{id}")
    public String updateCategory(@PathVariable("id") Long id,
            @ModelAttribute("categoryForm") CategoryRequestDto form,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            categoryService.updateCategory(id, form);
            redirectAttributes.addFlashAttribute("categorySuccess",
                    "Kateqoriya uğurla yeniləndi.");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            model.addAttribute("categoryError", ex.getMessage());
            model.addAttribute("editingCategoryId", id);
            List<CategoryDto> categories = categoryService.getAllCategories();
            model.addAttribute("categoryList", categories);
            return "admin/categories";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("categorySuccess",
                    "Kateqoriya uğurla silindi.");
        } catch (EntityNotFoundException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("categoryError",
                    ex.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
