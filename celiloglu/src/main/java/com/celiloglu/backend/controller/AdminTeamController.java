package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.response.AdminTeamMemberDto;
import com.celiloglu.backend.dto.response.AdminTeamSummaryDto;
import com.celiloglu.backend.enums.Role;
import com.celiloglu.backend.security.CustomUserDetails;
import com.celiloglu.backend.service.AdminTeamService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin/team")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminTeamController {

    private final AdminTeamService adminTeamService;

    public AdminTeamController(AdminTeamService adminTeamService) {
        this.adminTeamService = adminTeamService;
    }

    @GetMapping({ "", ".html" })
    public String showTeamPage(@RequestParam(name = "searchUsername", required = false) String searchUsername,
                               Model model) {
        AdminTeamSummaryDto summary = adminTeamService.getTeamSummary();
        model.addAttribute("summary", summary);
        model.addAttribute("activePage", "team");

        if (searchUsername != null && !searchUsername.trim().isEmpty()) {
            String trimmed = searchUsername.trim();
            Optional<AdminTeamMemberDto> foundUserOpt = adminTeamService.findActiveUserByExactUsername(trimmed);
            if (foundUserOpt.isPresent()) {
                model.addAttribute("foundUser", foundUserOpt.get());
            } else {
                model.addAttribute("searchNotFound", true);
            }
            model.addAttribute("searchedUsername", trimmed);
        }

        return "admin/team";
    }

    @PostMapping("/{id}/role")
    public String updateStaffRole(@PathVariable("id") Long id,
                                  @RequestParam("newRole") String newRoleStr,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new AccessDeniedException("Autentifikasiya olunmuş istifadəçi tapılmadı.");
        }

        Role newRole;
        try {
            newRole = Role.valueOf(newRoleStr.trim());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Yolverilməz rol dəyəri daxil edildi.");
            return "redirect:/admin/team";
        }

        try {
            boolean changed = adminTeamService.assignRole(id, newRole, userDetails.getId());
            if (changed) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "İstifadəçinin rolu uğurla '" + newRole.name() + "' olaraq yeniləndi. Əvvəlki sessiyaları etibarsız edildi.");
            } else {
                redirectAttributes.addFlashAttribute("infoMessage",
                        "İstifadəçi artıq '" + newRole.name() + "' rolundadır. Heç bir dəyişiklik edilmədi.");
            }
        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "İcazə xətası: " + ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gözlənilməz xəta baş verdi: " + ex.getMessage());
        }

        return "redirect:/admin/team";
    }
}

