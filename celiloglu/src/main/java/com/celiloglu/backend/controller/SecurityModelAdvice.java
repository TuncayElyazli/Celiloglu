package com.celiloglu.backend.controller;

import com.celiloglu.backend.security.CustomUserDetails;
import com.celiloglu.backend.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@ControllerAdvice
@Component("securityUtils")
public class SecurityModelAdvice {

    private static final String CART_TOTAL_ITEMS_CACHE_KEY = "SecurityModelAdvice.cartTotalItems.cached";

    private final CartService cartService;

    public SecurityModelAdvice() {
        this.cartService = null;
    }

    @Autowired
    public SecurityModelAdvice(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    @ModelAttribute("currentUserRole")
    public String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String role = ga.getAuthority();
            if (role.startsWith("ROLE_")) {
                return role.substring(5);
            }
        }
        return null;
    }

    @ModelAttribute("currentUserDisplayName")
    public String getCurrentUserDisplayName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getDisplayName();
        }
        return auth.getName();
    }

    @ModelAttribute("currentUsername")
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUsername();
        }
        return auth.getName();
    }

    @ModelAttribute("currentUserFullName")
    public String getCurrentUserFullName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getFullName();
        }
        return null;
    }

    @ModelAttribute("currentUserRoleLabel")
    public String getCurrentUserRoleLabel() {
        String role = getCurrentUserRole();
        if (role == null) {
            return null;
        }
        return switch (role) {
            case "SUPER_ADMIN" -> "Super Admin";
            case "ADMIN" -> "Admin";
            case "CASHIER" -> "Kassir";
            case "WAREHOUSE_MANAGER" -> "Anbar Müdiri";
            case "USER" -> "Müştəri";
            default -> role;
        };
    }

    @ModelAttribute("isSuperAdmin")
    public boolean isSuperAdmin() {
        return hasRole("ROLE_SUPER_ADMIN");
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    @ModelAttribute("isCashier")
    public boolean isCashier() {
        return hasRole("ROLE_CASHIER");
    }

    @ModelAttribute("isWarehouse")
    public boolean isWarehouse() {
        return hasRole("ROLE_WAREHOUSE_MANAGER");
    }

    @ModelAttribute("canAccessAdminDashboard")
    public boolean canAccessAdminDashboard() {
        return isSuperAdmin() || isAdmin();
    }

    @ModelAttribute("canAccessOrders")
    public boolean canAccessOrders() {
        return isSuperAdmin() || isAdmin() || isCashier() || isWarehouse();
    }

    @ModelAttribute("canAccessPayments")
    public boolean canAccessPayments() {
        return isSuperAdmin() || isAdmin() || isCashier();
    }

    @ModelAttribute("canAccessAnalytics")
    public boolean canAccessAnalytics() {
        return isSuperAdmin() || isAdmin() || isCashier();
    }

    @ModelAttribute("canAccessUsers")
    public boolean canAccessUsers() {
        return isSuperAdmin() || isAdmin();
    }

    @ModelAttribute("canAccessProducts")
    public boolean canAccessProducts() {
        return isSuperAdmin() || isAdmin();
    }

    @ModelAttribute("canChangeOrderStatus")
    public boolean canChangeOrderStatus() {
        return isSuperAdmin() || isAdmin() || isWarehouse();
    }

    @ModelAttribute("canRecordPayment")
    public boolean canRecordPayment() {
        return isSuperAdmin() || isAdmin() || isCashier();
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (role.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    @ModelAttribute("cartTotalItems")
    public Long getCartTotalItems() {
        return resolveCartTotalItems();
    }

    @ModelAttribute("cartItemCount")
    public Long getCartItemCount() {
        return resolveCartTotalItems();
    }

    private Long resolveCartTotalItems() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object cached = attributes.getAttribute(CART_TOTAL_ITEMS_CACHE_KEY, RequestAttributes.SCOPE_REQUEST);
            if (cached instanceof Long count) {
                return count;
            }
            Long calculated = computeCartTotalItems();
            attributes.setAttribute(CART_TOTAL_ITEMS_CACHE_KEY, calculated, RequestAttributes.SCOPE_REQUEST);
            return calculated;
        }
        return computeCartTotalItems();
    }

    private Long computeCartTotalItems() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken || cartService == null) {
            return 0L;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails && userDetails.getId() != null) {
            try {
                Long total = cartService.getCartTotalItems(userDetails.getId());
                return total != null ? total : 0L;
            } catch (Exception ex) {
                return 0L;
            }
        }
        return 0L;
    }
}
