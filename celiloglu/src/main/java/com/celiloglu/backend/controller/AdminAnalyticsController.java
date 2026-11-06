package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.response.B2BAnalyticsSummaryDto;
import com.celiloglu.backend.dto.response.B2BCustomerAnalyticsDto;
import com.celiloglu.backend.service.AdminAnalyticsService;
import com.celiloglu.backend.service.CustomerAnalyticsExcelExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;
    private final CustomerAnalyticsExcelExportService excelExportService;

    @Autowired
    public AdminAnalyticsController(AdminAnalyticsService analyticsService,
                                    CustomerAnalyticsExcelExportService excelExportService) {
        this.analyticsService = analyticsService;
        this.excelExportService = excelExportService;
    }

    @GetMapping({ "/admin/analytics", "/admin/analytics.html" })
    public String getAnalytics(@RequestParam(name = "page", defaultValue = "0") int page,
                               @RequestParam(name = "size", defaultValue = "10") int size,
                               Model model) {
        int pageNumber = Math.max(page, 0);
        int pageSize = (size > 0 && size <= 100) ? size : 10;

        B2BAnalyticsSummaryDto summary = analyticsService.getAnalyticsSummary();
        Page<B2BCustomerAnalyticsDto> customersPage = analyticsService.getCustomerAnalytics(pageNumber, pageSize);

        model.addAttribute("summary", summary);
        model.addAttribute("customersPage", customersPage);
        model.addAttribute("customers", customersPage.getContent());
        model.addAttribute("activePage", "analytics");

        return "admin/analytics";
    }

    @GetMapping("/admin/analytics/export/excel")
    public ResponseEntity<byte[]> exportCustomerAnalyticsToExcel() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean hasRole = auth.getAuthorities().stream().anyMatch(a -> {
            String role = a.getAuthority();
            return "ROLE_ADMIN".equals(role) || "ROLE_SUPER_ADMIN".equals(role) || "ROLE_CASHIER".equals(role);
        });
        if (!hasRole) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        byte[] excelData = excelExportService.exportCustomerAnalyticsToExcel();
        String filename = "musteri_hesablasmalari_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }
}
