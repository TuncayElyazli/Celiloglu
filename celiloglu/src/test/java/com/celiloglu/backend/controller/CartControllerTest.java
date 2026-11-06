package com.celiloglu.backend.controller;

import com.celiloglu.backend.config.PasswordConfig;
import com.celiloglu.backend.config.SecurityConfig;
import com.celiloglu.backend.dto.request.CartItemRequestDto;
import com.celiloglu.backend.dto.response.CartItemResponseDto;
import com.celiloglu.backend.dto.response.CartResponseDto;
import com.celiloglu.backend.repository.UserRepository;
import com.celiloglu.backend.security.CustomUserDetails;
import com.celiloglu.backend.security.CustomUserDetailsService;
import com.celiloglu.backend.service.CartService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        SecurityConfig.class,
        PasswordConfig.class,
        CustomUserDetailsService.class,
        CartControllerTest.TestConfig.class
})
@WebAppConfiguration
class CartControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CartService cartService;

    private MockMvc mockMvc;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        reset(cartService);

        userDetails = new CustomUserDetails(
                15L,
                "tuncay",
                "Password123!",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("GET /cart anonim istifadəçi üçün /login səhifəsinə yönləndirməlidir")
    void testCartPage_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /cart autentifikasiya olunmuş istifadəçi üçün principal ID ilə səbəti yükləməlidir")
    void testCartPage_Authenticated_LoadsCartWithPrincipalId() throws Exception {
        CartItemResponseDto item = new CartItemResponseDto();
        item.setId(1L);
        item.setProductId(101L);
        item.setProductName("Əyləc diski");
        item.setProductCode("BR-8492");
        item.setUnitPrice(new BigDecimal("75.50"));
        item.setQuantity(2);
        item.setTotalPrice(new BigDecimal("151.00"));
        item.setAvailable(true);

        CartResponseDto cartDto = new CartResponseDto(List.of(item), 2L, new BigDecimal("151.00"));
        when(cartService.getCart(15L)).thenReturn(cartDto);

        mockMvc.perform(get("/cart").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("cart"))
                .andExpect(model().attribute("cart", cartDto));

        verify(cartService).getCart(15L);
    }

    @Test
    @DisplayName("GET /cart.html marşrutu da eyni şəkildə principal ID ilə səbəti yükləməlidir")
    void testCartHtml_Authenticated_LoadsCartWithPrincipalId() throws Exception {
        CartResponseDto emptyCart = new CartResponseDto(Collections.emptyList(), 0L, BigDecimal.ZERO);
        when(cartService.getCart(15L)).thenReturn(emptyCart);

        mockMvc.perform(get("/cart.html").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("cart", emptyCart));

        verify(cartService).getCart(15L);
    }

    @Test
    @DisplayName("GET /cart sorğusunda URL-də verilən userId parametri nəzərə alınmır, yalnız principal ID istifadə olunur")
    void testCartPage_IgnoresUserIdParamFromUrl() throws Exception {
        CartResponseDto cartDto = new CartResponseDto(Collections.emptyList(), 0L, BigDecimal.ZERO);
        when(cartService.getCart(15L)).thenReturn(cartDto);

        mockMvc.perform(get("/cart?userId=999").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));

        verify(cartService, times(1)).getCart(15L);
        verify(cartService, never()).getCart(999L);
    }

    @Test
    @DisplayName("GET /cart zamanı CartService xəta atdıqda səhifə qırılmır, errorMessage ilə göstərilir")
    void testCartPage_ServiceThrowsException_HandledGracefully() throws Exception {
        when(cartService.getCart(15L)).thenThrow(new IllegalStateException("İstifadəçi hesabı deaktivdir!"));

        mockMvc.perform(get("/cart").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", "İstifadəçi hesabı deaktivdir!"))
                .andExpect(model().attributeExists("cart"));
    }

    @Test
    @DisplayName("POST /cart/items/add CSRF ilə uğurlu olduqda productId və quantity ilə CartService çağırır və redirect edir")
    void testAddItem_Success_WithCsrf() throws Exception {
        when(cartService.addItemToCart(eq(15L), any(CartItemRequestDto.class)))
                .thenReturn(new CartResponseDto());

        mockMvc.perform(post("/cart/items/add")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("productId", "101")
                        .param("quantity", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("successMessage", "Məhsul səbətə əlavə edildi."));

        verify(cartService).addItemToCart(eq(15L), argThat(req ->
                req != null && Long.valueOf(101L).equals(req.getProductId()) && Integer.valueOf(3).equals(req.getQuantity())
        ));
    }

    @Test
    @DisplayName("POST /cart/items/add parametr olaraq göndərilən userId-ni ignor edir və yalnız principal ID-dən istifadə edir")
    void testAddItem_IgnoresUserIdParamFromFormOrUrl() throws Exception {
        when(cartService.addItemToCart(eq(15L), any(CartItemRequestDto.class)))
                .thenReturn(new CartResponseDto());

        mockMvc.perform(post("/cart/items/add?userId=999")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("productId", "101")
                        .param("quantity", "2")
                        .param("userId", "888"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("successMessage", "Məhsul səbətə əlavə edildi."));

        verify(cartService).addItemToCart(eq(15L), any(CartItemRequestDto.class));
        verify(cartService, never()).addItemToCart(eq(999L), any(CartItemRequestDto.class));
        verify(cartService, never()).addItemToCart(eq(888L), any(CartItemRequestDto.class));
    }

    @Test
    @DisplayName("POST /cart/items/add CSRF olmadan göndərildikdə 403 Forbidden qaytarır")
    void testAddItem_WithoutCsrf_Returns403() throws Exception {
        mockMvc.perform(post("/cart/items/add")
                        .with(user(userDetails))
                        .param("productId", "101")
                        .param("quantity", "1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items/add anonim istifadəçi üçün /login ünvanına yönləndirir")
    void testAddItem_Anonymous_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/cart/items/add")
                        .with(csrf())
                        .param("productId", "101")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items/add mənfi və ya sıfır productId olduqda flash xəta mesajı verir")
    void testAddItem_InvalidProductId_ShowsErrorMessage() throws Exception {
        mockMvc.perform(post("/cart/items/add")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("productId", "0")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("errorMessage", "Məhsul ID-si müsbət ədəd olmalıdır!"));

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items/add 1-dən az miqdar olduqda flash xəta mesajı verir")
    void testAddItem_InvalidQuantity_ShowsErrorMessage() throws Exception {
        mockMvc.perform(post("/cart/items/add")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("productId", "101")
                        .param("quantity", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("errorMessage", "Məhsul sayı ən azı 1 olmalıdır!"));

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items/add servis xətası (stok bitib və s.) zamanı 500 atmır, errorMessage ilə yönləndirir")
    void testAddItem_ServiceThrowsException_ShowsErrorMessage() throws Exception {
        when(cartService.addItemToCart(eq(15L), any(CartItemRequestDto.class)))
                .thenThrow(new IllegalStateException("Məhsul stokda yoxdur!"));

        mockMvc.perform(post("/cart/items/add")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("productId", "101")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("errorMessage", "Məhsul stokda yoxdur!"));
    }

    @Test
    @DisplayName("POST /cart/items/add qeyri-rəqəm parametr göndərildikdə 500 atmır, flash errorMessage verir")
    void testAddItem_TypeMismatch_ShowsErrorMessage() throws Exception {
        mockMvc.perform(post("/cart/items/add")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("productId", "herf-deyer")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("errorMessage", "Daxil edilən parametr düzgün rəqəm formatında olmalıdır!"));

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items/add-by-code CSRF ilə uğurlu olduqda redirect və flash mesaj qaytarır")
    void testAddByCode_Success_WithCsrf() throws Exception {
        when(cartService.addItemByCode(15L, "BR-8492", 2))
                .thenReturn(new CartResponseDto());

        mockMvc.perform(post("/cart/items/add-by-code")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("code", "BR-8492")
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("successMessage", "Məhsul səbətə əlavə edildi."));

        verify(cartService).addItemByCode(15L, "BR-8492", 2);
    }

    @Test
    @DisplayName("POST /cart/items/add-by-code CSRF olmadan göndərildikdə 403 Forbidden qaytarır")
    void testAddByCode_WithoutCsrf_Returns403() throws Exception {
        mockMvc.perform(post("/cart/items/add-by-code")
                        .with(user(userDetails))
                        .param("code", "BR-8492")
                        .param("quantity", "1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items/add-by-code boş kod olduqda xəta flash mesajı qaytarır")
    void testAddByCode_EmptyCode_ShowsErrorMessage() throws Exception {
        mockMvc.perform(post("/cart/items/add-by-code")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("code", "   ")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("errorMessage", "Məhsul kodu boş ola bilməz!"));

        verify(cartService, never()).addItemByCode(anyLong(), anyString(), anyInt());
    }

    @Test
    @DisplayName("POST /cart/items/add-by-code tapılmayan kod zamanı EntityNotFoundException servis xətasını göstərir")
    void testAddByCode_EntityNotFound_ShowsErrorMessage() throws Exception {
        when(cartService.addItemByCode(15L, "NOT-EXIST", 1))
                .thenThrow(new EntityNotFoundException("Məhsul tapılmadı! Kod: NOT-EXIST"));

        mockMvc.perform(post("/cart/items/add-by-code")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("code", "NOT-EXIST")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("errorMessage", "Məhsul tapılmadı! Kod: NOT-EXIST"));

        verify(cartService).addItemByCode(15L, "NOT-EXIST", 1);
    }

    @Test
    @DisplayName("POST /cart/items/update CSRF ilə miqdarı yeniləyir və redirect edir")
    void testUpdateQuantity_Success_WithCsrf() throws Exception {
        when(cartService.updateItemQuantity(15L, 101L, 4))
                .thenReturn(new CartResponseDto());

        mockMvc.perform(post("/cart/items/update")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("productId", "101")
                        .param("quantity", "4")
                        .param("userId", "888")) // Hacking cəhdi - ignor edilməlidir
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("successMessage", "Məhsulun miqdarı yeniləndi."));

        verify(cartService).updateItemQuantity(15L, 101L, 4);
        verify(cartService, never()).updateItemQuantity(eq(888L), anyLong(), anyInt());
    }

    @Test
    @DisplayName("POST /cart/items/update CSRF olmadan 403 Forbidden qaytarır")
    void testUpdateQuantity_WithoutCsrf_Returns403() throws Exception {
        mockMvc.perform(post("/cart/items/update")
                        .with(user(userDetails))
                        .param("productId", "101")
                        .param("quantity", "4"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items/update miqdar 1-dən az olduqda servis çağırılmır və xəta verilir")
    void testUpdateQuantity_InvalidQuantity_ShowsErrorMessage() throws Exception {
        mockMvc.perform(post("/cart/items/update")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("productId", "101")
                        .param("quantity", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("errorMessage", "Məhsul sayı ən azı 1 olmalıdır!"));

        verify(cartService, never()).updateItemQuantity(anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("POST /cart/items/update stok çatışmazlığı xətası zamanı flash mesaj göstərilir")
    void testUpdateQuantity_StockExceeded_ShowsErrorMessage() throws Exception {
        when(cartService.updateItemQuantity(15L, 101L, 99))
                .thenThrow(new IllegalArgumentException("Tələb olunan miqdar (99) mövcud stokdan (10) çox ola bilməz!"));

        mockMvc.perform(post("/cart/items/update")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("productId", "101")
                        .param("quantity", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("errorMessage", "Tələb olunan miqdar (99) mövcud stokdan (10) çox ola bilməz!"));
    }

    @Test
    @DisplayName("POST /cart/items/remove CSRF ilə məhsulu silir və redirect edir")
    void testRemoveItem_Success_WithCsrf() throws Exception {
        when(cartService.removeItemFromCart(15L, 101L))
                .thenReturn(new CartResponseDto());

        mockMvc.perform(post("/cart/items/remove")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("productId", "101"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("successMessage", "Məhsul səbətdən silindi."));

        verify(cartService).removeItemFromCart(15L, 101L);
    }

    @Test
    @DisplayName("POST /cart/items/remove CSRF olmadan 403 Forbidden qaytarır")
    void testRemoveItem_WithoutCsrf_Returns403() throws Exception {
        mockMvc.perform(post("/cart/items/remove")
                        .with(user(userDetails))
                        .param("productId", "101"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/clear CSRF ilə səbəti təmizləyir və redirect edir")
    void testClearCart_Success_WithCsrf() throws Exception {
        doNothing().when(cartService).clearCart(15L);

        mockMvc.perform(post("/cart/clear")
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("successMessage", "Səbət uğurla təmizləndi."));

        verify(cartService).clearCart(15L);
    }

    @Test
    @DisplayName("POST /cart/clear CSRF olmadan 403 Forbidden qaytarır")
    void testClearCart_WithoutCsrf_Returns403() throws Exception {
        mockMvc.perform(post("/cart/clear")
                        .with(user(userDetails)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("Rəqəm çevrilməsi xətası (Type Mismatch) istifadəçiyə aydın flash mesaj ilə göstərilir")
    void testTypeMismatch_NumberFormatException_HandledGracefully() throws Exception {
        mockMvc.perform(post("/cart/items/update")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("productId", "qeyri-reqem")
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("errorMessage", "Daxil edilən parametr düzgün rəqəm formatında olmalıdır!"));

        verifyNoInteractions(cartService);
    }

    @Configuration
    @EnableWebMvc
    static class TestConfig {

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        public CartService cartService() {
            return mock(CartService.class);
        }

        @Bean
        public CartController cartController(CartService cartService) {
            return new CartController(cartService);
        }

        @Bean
        public ViewResolver viewResolver() {
            org.thymeleaf.templateresolver.ClassLoaderTemplateResolver templateResolver =
                    new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
            templateResolver.setPrefix("templates/");
            templateResolver.setSuffix(".html");
            templateResolver.setTemplateMode(org.thymeleaf.templatemode.TemplateMode.HTML);
            templateResolver.setCharacterEncoding("UTF-8");

            org.thymeleaf.spring6.SpringTemplateEngine templateEngine = new org.thymeleaf.spring6.SpringTemplateEngine();
            templateEngine.setTemplateResolver(templateResolver);

            org.thymeleaf.spring6.view.ThymeleafViewResolver resolver = new org.thymeleaf.spring6.view.ThymeleafViewResolver();
            resolver.setTemplateEngine(templateEngine);
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
    }
}
