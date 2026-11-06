package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.request.UserCreateRequestDto;
import com.celiloglu.backend.dto.request.UserUpdateRequestDto;
import com.celiloglu.backend.dto.response.UserResponseDto;
import com.celiloglu.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(@RequestParam(name = "page", defaultValue = "0") int page,
                            Model model) {
        populateUsersModel(model, page, new UserCreateRequestDto(), new UserUpdateRequestDto(), null, false);
        return "admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editUser(@PathVariable("id") Long id,
                           @RequestParam(name = "page", defaultValue = "0") int page,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            UserResponseDto user = userService.getUserById(id);
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getAuthorities() != null) {
                boolean isSuperAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
                boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                if (isAdmin && !isSuperAdmin && !"USER".equals(user.getRole())) {
                    redirectAttributes.addFlashAttribute("userError", "Admin yalnız adi müştəri (USER) hesablarını redaktə edə bilər!");
                    return "redirect:/admin/users";
                }
            }

            UserUpdateRequestDto updateForm = new UserUpdateRequestDto();
            updateForm.setUsername(user.getUsername());
            updateForm.setEmail(user.getEmail());
            updateForm.setFirstName(user.getFirstName());
            updateForm.setLastName(user.getLastName());
            updateForm.setPhone(user.getPhone());
            updateForm.setAddress(user.getAddress());
            updateForm.setCompanyName(user.getCompanyName());
            updateForm.setTin(user.getTin());

            populateUsersModel(model, page, new UserCreateRequestDto(), updateForm, id, true);
            return "admin/users";
        } catch (EntityNotFoundException | IllegalArgumentException | org.springframework.security.access.AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("userError", ex.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping
    public String createUser(@ModelAttribute("createForm") UserCreateRequestDto form,
                             @RequestParam(name = "page", defaultValue = "0") int page,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(form);
            redirectAttributes.addFlashAttribute("userSuccess", "İstifadəçi uğurla yaradıldı.");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            form.setPassword("");
            model.addAttribute("userError", ex.getMessage());
            populateUsersModel(model, page, form, new UserUpdateRequestDto(), null, false);
            return "admin/users";
        } catch (Exception ex) {
            form.setPassword("");
            model.addAttribute("userError", "Gözlənilməz xəta baş verdi: " + ex.getMessage());
            populateUsersModel(model, page, form, new UserUpdateRequestDto(), null, false);
            return "admin/users";
        }
    }

    @PostMapping("/{id}")
    public String updateUser(@PathVariable("id") Long id,
                             @ModelAttribute("updateForm") UserUpdateRequestDto form,
                             @RequestParam(name = "page", defaultValue = "0") int page,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, form);
            redirectAttributes.addFlashAttribute("userSuccess", "İstifadəçi məlumatları uğurla yeniləndi.");
            return "redirect:/admin/users";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("userError", ex.getMessage());
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("userError", ex.getMessage());
            populateUsersModel(model, page, new UserCreateRequestDto(), form, id, true);
            return "admin/users";
        } catch (Exception ex) {
            model.addAttribute("userError", "Gözlənilməz xəta baş verdi: " + ex.getMessage());
            populateUsersModel(model, page, new UserCreateRequestDto(), form, id, true);
            return "admin/users";
        }
    }

    @PostMapping("/{id}/activate")
    public String activateUser(@PathVariable("id") Long id,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.setUserActive(id, true);
            redirectAttributes.addFlashAttribute("userSuccess", "İstifadəçi uğurla aktivləşdirildi.");
        } catch (EntityNotFoundException | IllegalArgumentException | org.springframework.security.access.AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("userError", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivateUser(@PathVariable("id") Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            userService.setUserActive(id, false);
            redirectAttributes.addFlashAttribute("userSuccess", "İstifadəçi uğurla deaktiv edildi.");
        } catch (EntityNotFoundException | IllegalArgumentException | org.springframework.security.access.AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("userError", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    private void populateUsersModel(Model model, int page,
                                    UserCreateRequestDto createForm,
                                    UserUpdateRequestDto updateForm,
                                    Long editingId,
                                    boolean isEditMode) {
        int targetPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(targetPage, 10, Sort.by(Sort.Direction.DESC, "id"));
        Page<UserResponseDto> userPage = userService.getAllUsers(pageable);

        model.addAttribute("userPage", userPage);
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", targetPage);

        if (!model.containsAttribute("createForm")) {
            model.addAttribute("createForm", createForm);
        }
        if (!model.containsAttribute("updateForm")) {
            model.addAttribute("updateForm", updateForm);
        }
        model.addAttribute("editingUserId", editingId);
        model.addAttribute("isEditMode", isEditMode);
    }
}
