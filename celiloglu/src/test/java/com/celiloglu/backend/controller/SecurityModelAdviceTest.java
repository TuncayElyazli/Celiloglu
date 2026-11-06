package com.celiloglu.backend.controller;

import com.celiloglu.backend.security.CustomUserDetails;
import com.celiloglu.backend.service.CartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityModelAdviceTest {

    private final SecurityModelAdvice advice = new SecurityModelAdvice();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Anonim və ya boş kontekstdə isAuthenticated false qaytarır")
    void testIsAuthenticated_anonymousOrNull() {
        SecurityContextHolder.clearContext();
        assertFalse(advice.isAuthenticated());

        AnonymousAuthenticationToken anonToken = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonToken);
        assertFalse(advice.isAuthenticated());
    }

    @Test
    @DisplayName("Autentifikasiya olunmuş istifadəçi üçün isAuthenticated true qaytarır")
    void testIsAuthenticated_authenticatedUser() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                "tuncay", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(token);
        assertTrue(advice.isAuthenticated());
    }

    @Test
    @DisplayName("Anonim istifadəçi üçün getCartTotalItems və getCartItemCount 0 qaytarır, servis çağırılmır")
    void testGetCartTotalItems_anonymous() {
        SecurityContextHolder.clearContext();
        CartService mockCartService = mock(CartService.class);
        SecurityModelAdvice adviceWithService = new SecurityModelAdvice(mockCartService);

        assertEquals(0L, adviceWithService.getCartTotalItems());
        assertEquals(0L, adviceWithService.getCartItemCount());
        verifyNoInteractions(mockCartService);
    }

    @Test
    @DisplayName("Autentifikasiya olunmuş istifadəçi üçün tək sorğuda sayğac bir dəfə hesablanır, hər iki atribut eynidir və getCart çağırılmır")
    void testGetCartTotalItems_authenticated() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        CustomUserDetails userDetails = new CustomUserDetails(15L, "tuncay", "pass", true, List.of());
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                userDetails, "pass", userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);

        CartService mockCartService = mock(CartService.class);
        when(mockCartService.getCartTotalItems(15L)).thenReturn(7L);

        SecurityModelAdvice adviceWithService = new SecurityModelAdvice(mockCartService);
        Long totalItems = adviceWithService.getCartTotalItems();
        Long itemCount = adviceWithService.getCartItemCount();

        // 1. Hər iki atribut miqdar cəmini qaytarır
        assertEquals(7L, totalItems);
        assertEquals(7L, itemCount);
        // 2. Hər iki atributun dəyəri eynidir
        assertEquals(totalItems, itemCount);

        // 3. Bir HTTP sorğusunda sayğac yalnız bir dəfə hesablanır
        verify(mockCartService, times(1)).getCartTotalItems(15L);
        // 4. Sayğac üçün getCart heç vaxt çağırılmır
        verify(mockCartService, never()).getCart(any());
    }

    @Test
    @DisplayName("Servis xəta atdıqda getCartTotalItems 0 qaytarır, xəta fırlatmır və getCart çağırılmır")
    void testGetCartTotalItems_serviceThrowsException_returnsZero() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        CustomUserDetails userDetails = new CustomUserDetails(15L, "tuncay", "pass", true, List.of());
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                userDetails, "pass", userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);

        CartService mockCartService = mock(CartService.class);
        when(mockCartService.getCartTotalItems(15L)).thenThrow(new IllegalStateException("İstifadəçi hesabı deaktivdir!"));

        SecurityModelAdvice adviceWithService = new SecurityModelAdvice(mockCartService);
        assertEquals(0L, adviceWithService.getCartTotalItems());
        assertEquals(0L, adviceWithService.getCartItemCount());
        verify(mockCartService, never()).getCart(any());
    }
}
