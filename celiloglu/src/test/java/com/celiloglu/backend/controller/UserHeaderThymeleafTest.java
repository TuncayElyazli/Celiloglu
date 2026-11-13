package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.response.CartResponseDto;
import com.celiloglu.backend.dto.response.OrderResponseDto;
import com.celiloglu.backend.dto.response.ProductResponseDto;
import com.celiloglu.backend.security.CustomUserDetails;
import com.celiloglu.backend.service.CartService;
import com.celiloglu.backend.service.OrderService;
import com.celiloglu.backend.service.ProductFormOptionsService;
import com.celiloglu.backend.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserHeaderThymeleafTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductFormOptionsService optionsService;

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        viewResolver.setCharacterEncoding("UTF-8");

        SecurityModelAdvice securityAdvice = new SecurityModelAdvice(cartService);

        HomeController homeController = new HomeController(productService, optionsService);
        CartController cartController = new CartController(cartService);
        OrderController orderController = new OrderController(orderService);
        ProductController productController = new ProductController(productService);

        mockMvc = MockMvcBuilders.standaloneSetup(homeController, cartController, orderController, productController)
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        new org.springframework.data.web.PageableHandlerMethodArgumentResolver()
                )
                .setControllerAdvice(securityAdvice)
                .setViewResolvers(viewResolver)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(CustomUserDetails userDetails) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Ana səhifə (index.html): Giriş etmiş istifadəçi üçün profil göstəricisi və Çıxış render olunur")
    void testIndexPage_WhenAuthenticated_RendersProfileIndicator() throws Exception {
        CustomUserDetails user = new CustomUserDetails(
                1L, "tuncay_user", "password123", "Tuncay", "Elyazlı", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authenticateUser(user);

        when(optionsService.getCategories()).thenReturn(Collections.emptyList());
        when(optionsService.getCarBrands()).thenReturn(Collections.emptyList());
        when(optionsService.getYears()).thenReturn(Collections.emptyList());
        when(productService.searchActiveProducts(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("nav-profile-circle")))
                .andExpect(content().string(containsString("Tuncay Elyazlı")))
                .andExpect(content().string(containsString("Çıxış")))
                .andExpect(content().string(not(containsString(">Daxil ol<"))));
    }

    @Test
    @DisplayName("Ana səhifə (index.html): Yalnız username olduqda profil göstəricisində username əks olunur")
    void testIndexPage_WhenUsernameOnly_RendersUsername() throws Exception {
        CustomUserDetails user = new CustomUserDetails(
                2L, "alibey", "password123", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authenticateUser(user);

        when(optionsService.getCategories()).thenReturn(Collections.emptyList());
        when(optionsService.getCarBrands()).thenReturn(Collections.emptyList());
        when(optionsService.getYears()).thenReturn(Collections.emptyList());
        when(productService.searchActiveProducts(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("nav-profile-circle")))
                .andExpect(content().string(containsString("alibey")))
                .andExpect(content().string(containsString("Çıxış")));
    }

    @Test
    @DisplayName("Səbət (cart.html): Giriş etmiş istifadəçi üçün profil göstəricisi və Çıxış tam render olunur")
    void testCartPage_WhenAuthenticated_RendersProfileIndicator() throws Exception {
        CustomUserDetails user = new CustomUserDetails(
                1L, "tuncay_user", "password123", "Tuncay", "Elyazlı", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authenticateUser(user);

        CartResponseDto emptyCart = new CartResponseDto();
        emptyCart.setTotalItems(0L);
        when(cartService.getCart(1L)).thenReturn(emptyCart);

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(content().string(containsString("nav-profile-circle")))
                .andExpect(content().string(containsString("Tuncay Elyazlı")))
                .andExpect(content().string(containsString("Səbətiniz")))
                .andExpect(content().string(containsString("Çıxış")));
    }

    @Test
    @DisplayName("Ana səhifə: Giriş etməmiş anonim istifadəçi üçün login-ə yönləndirmə baş verir")
    void testIndexPage_WhenAnonymous_RedirectsToLogin() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Məhsul detalları (product-details.html): Giriş etmiş istifadəçi üçün profil göstəricisi render olunur")
    void testProductDetailsPage_WhenAuthenticated_RendersProfileIndicator() throws Exception {
        CustomUserDetails user = new CustomUserDetails(
                1L, "tuncay_user", "password123", "Tuncay", "Elyazlı", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authenticateUser(user);

        ProductResponseDto product = new ProductResponseDto();
        product.setId(10L);
        product.setName("Test Məhsul");
        product.setCode("TST-10");
        product.setPrice(new BigDecimal("45.00"));
        product.setStockQuantity(5);
        when(productService.getActiveProductById(10L)).thenReturn(product);

        mockMvc.perform(get("/products/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-details"))
                .andExpect(content().string(containsString("nav-profile-circle")))
                .andExpect(content().string(containsString("Tuncay Elyazlı")))
                .andExpect(content().string(containsString("Çıxış")));
    }

    @Test
    @DisplayName("Sifarişlər (orders.html): Giriş etmiş istifadəçi üçün profil göstəricisi və Çıxış render olunur")
    void testOrdersPage_WhenAuthenticated_RendersProfileIndicator() throws Exception {
        CustomUserDetails user = new CustomUserDetails(
                1L, "tuncay_user", "password123", "Tuncay", "Elyazlı", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authenticateUser(user);

        when(orderService.getUserOrders(eq(1L), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(content().string(containsString("nav-profile-circle")))
                .andExpect(content().string(containsString("Tuncay Elyazlı")))
                .andExpect(content().string(containsString("Çıxış")));
    }
}

