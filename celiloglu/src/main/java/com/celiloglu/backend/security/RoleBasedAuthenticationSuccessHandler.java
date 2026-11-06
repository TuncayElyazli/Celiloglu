package com.celiloglu.backend.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleBasedAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        boolean isCashier = hasRole(authentication, "ROLE_CASHIER");
        boolean isWarehouse = hasRole(authentication, "ROLE_WAREHOUSE_MANAGER");
        boolean isAdminOrSuper = hasRole(authentication, "ROLE_ADMIN") || hasRole(authentication, "ROLE_SUPER_ADMIN");

        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            if (isCashier && (targetUrl.contains("/admin/payments") || targetUrl.contains("/admin/analytics") || targetUrl.contains("/admin/orders"))) {
                super.onAuthenticationSuccess(request, response, authentication);
                return;
            }
            if (isWarehouse && targetUrl.contains("/admin/orders")) {
                super.onAuthenticationSuccess(request, response, authentication);
                return;
            }
            if (isAdminOrSuper) {
                super.onAuthenticationSuccess(request, response, authentication);
                return;
            }
            if (!isCashier && !isWarehouse && !isAdminOrSuper && !targetUrl.contains("/admin")) {
                super.onAuthenticationSuccess(request, response, authentication);
                return;
            }
            requestCache.removeRequest(request, response);
        }

        clearAuthenticationAttributes(request);
        String redirectUrl;
        if (isCashier) {
            redirectUrl = "/admin/payments";
        } else if (isWarehouse) {
            redirectUrl = "/admin/orders";
        } else if (isAdminOrSuper) {
            redirectUrl = "/admin";
        } else {
            redirectUrl = "/";
        }
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (role.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}

