package com.celiloglu.backend.controller;

import com.celiloglu.backend.config.PasswordConfig;
import com.celiloglu.backend.config.SecurityConfig;
import com.celiloglu.backend.dto.response.OrderItemResponseDto;
import com.celiloglu.backend.dto.response.OrderResponseDto;
import com.celiloglu.backend.enums.OrderStatus;
import com.celiloglu.backend.enums.PaymentMethod;
import com.celiloglu.backend.enums.PaymentStatus;
import com.celiloglu.backend.repository.UserRepository;
import com.celiloglu.backend.security.CustomUserDetails;
import com.celiloglu.backend.security.CustomUserDetailsService;
import com.celiloglu.backend.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        OrderControllerTest.TestConfig.class
})
@WebAppConfiguration
class OrderControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private OrderService orderService;

    private MockMvc mockMvc;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        reset(orderService);

        userDetails = new CustomUserDetails(
                15L,
                "tuncay",
                "Password123!",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("GET /orders anonim istifadəçi üçün /login səhifəsinə yönləndirməlidir")
    void testOrdersPage_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("GET /orders autentifikasiya olunmuş istifadəçi üçün yalnız Principal-dakı ID-dən istifadə edir")
    void testOrdersPage_Authenticated_UsesPrincipalId() throws Exception {
        when(orderService.getUserOrders(eq(15L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/orders").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("ordersPage"))
                .andExpect(model().attributeExists("orders"));

        verify(orderService).getUserOrders(eq(15L), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /orders.html marşrutu da eyni şəkildə işləməlidir")
    void testOrdersHtml_Authenticated_UsesPrincipalId() throws Exception {
        when(orderService.getUserOrders(eq(15L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/orders.html").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"));

        verify(orderService).getUserOrders(eq(15L), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /orders URL parametrlərində göndərilən yad userId-ni qəbul etmir və yalnız Principal ID istifadə edir")
    void testOrdersPage_IgnoresForeignUserIdInUrl() throws Exception {
        when(orderService.getUserOrders(eq(15L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/orders?userId=999").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"));

        // Heç vaxt 999 istifadə edilməməlidir
        verify(orderService).getUserOrders(eq(15L), any(Pageable.class));
        verify(orderService, never()).getUserOrders(eq(999L), any());
    }

    @Test
    @DisplayName("GET /orders səhifələmə parametrlərini servisə düzgün ötürür")
    void testOrdersPage_PaginationPassedToService() throws Exception {
        when(orderService.getUserOrders(eq(15L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(2, 5), 0));

        mockMvc.perform(get("/orders?page=2&size=5").with(user(userDetails)))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).getUserOrders(eq(15L), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("POST /orders CSRF olmadan 403 Forbidden qaytarmalıdır")
    void testCreateOrder_WithoutCsrf_Returns403Forbidden() throws Exception {
        mockMvc.perform(post("/orders")
                        .with(user(userDetails))
                        .param("shippingAddress", "Bakı şəhəri"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("POST /orders uğurlu olduqda /orders ünvanına redirect edir və sifariş nömrəsi ilə flash mesaj göstərir")
    void testCreateOrder_Success_RedirectsToOrdersWithFlashMessage() throws Exception {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(101L);
        dto.setOrderNumber("ORD-987654321012");
        dto.setStatus(OrderStatus.GÖZLƏYİR);
        dto.setTotalAmount(new BigDecimal("150.00"));

        when(orderService.createOrderFromCart(eq(15L), eq("Nərimanov ray."), eq("Təcili çatdırılma")))
                .thenReturn(dto);

        mockMvc.perform(post("/orders")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("shippingAddress", "Nərimanov ray.")
                        .param("orderNotes", "Təcili çatdırılma"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attribute("successMessage", containsString("ORD-987654321012")));

        verify(orderService).createOrderFromCart(eq(15L), eq("Nərimanov ray."), eq("Təcili çatdırılma"));
    }

    @Test
    @DisplayName("POST /orders brauzerdən göndərilən yad userId, qiymət və status parametrlərini nəzərə almır")
    void testCreateOrder_IgnoresForeignUserIdPriceAndStatuses() throws Exception {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(102L);
        dto.setOrderNumber("ORD-XYZ112233");

        when(orderService.createOrderFromCart(eq(15L), eq("Nəsimi ray."), isNull()))
                .thenReturn(dto);

        mockMvc.perform(post("/orders")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("userId", "999")
                        .param("totalAmount", "0.01")
                        .param("price", "0.01")
                        .param("status", "ÇATDIRILDI")
                        .param("shippingAddress", "Nəsimi ray."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        // Yalnız Principal ID-si (15L) və forma mətnləri ötürülməlidir
        verify(orderService).createOrderFromCart(eq(15L), eq("Nəsimi ray."), isNull());
        verify(orderService, never()).createOrderFromCart(eq(999L), any(), any());
    }

    @Test
    @DisplayName("POST /orders servis xətası (IllegalStateException) zamanı /cart-a qayıdır və forma dəyərlərini saxlayır")
    void testCreateOrder_ServiceException_RedirectsToCartWithFlashErrorAndFormValues() throws Exception {
        when(orderService.createOrderFromCart(eq(15L), any(), any()))
                .thenThrow(new IllegalStateException("Səbət boşdur!"));

        mockMvc.perform(post("/orders")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("shippingAddress", "Xətai ray.")
                        .param("orderNotes", "Qeyd 123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("errorMessage", "Səbət boşdur!"))
                .andExpect(flash().attribute("shippingAddress", "Xətai ray."))
                .andExpect(flash().attribute("orderNotes", "Qeyd 123"));
    }

    @Test
    @DisplayName("POST /orders tapılmayan varlıq xətası (EntityNotFoundException) zamanı /cart-a qayıdır")
    void testCreateOrder_EntityNotFoundException_RedirectsToCart() throws Exception {
        when(orderService.createOrderFromCart(eq(15L), any(), any()))
                .thenThrow(new EntityNotFoundException("Məhsul tapılmadı!"));

        mockMvc.perform(post("/orders")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("shippingAddress", "Ünvan"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("errorMessage", "Məhsul tapılmadı!"))
                .andExpect(flash().attribute("shippingAddress", "Ünvan"));
    }

    @Test
    @DisplayName("POST /orders gözlənilməz xəta zamanı /cart-a qayıdır və uğur mesajı göstərmir")
    void testCreateOrder_UnexpectedException_HandledGracefully() throws Exception {
        when(orderService.createOrderFromCart(eq(15L), any(), any()))
                .thenThrow(new RuntimeException("Baza əlaqəsi kəsildi"));

        mockMvc.perform(post("/orders")
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("errorMessage", "Sifariş yaradılarkən xəta baş verdi. Zəhmət olmasa yenidən cəhd edin."))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertFalse(result.getFlashMap().containsKey("successMessage")));
    }

    @Test
    @DisplayName("GET /orders heç bir halda sifariş yaratmamalıdır")
    void testGetOrders_DoesNotCreateOrder() throws Exception {
        when(orderService.getUserOrders(eq(15L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/orders").with(user(userDetails)))
                .andExpect(status().isOk());

        verify(orderService, never()).createOrderFromCart(any(), any(), any());
    }

    @Test
    @DisplayName("orders.html şablonu sifariş siyahısı və boş siyahı görünüşünü düzgün render edir")
    void testOrdersTemplate_Rendering() throws Exception {
        // 1. Siyahı mövcud olduqda render yoxlaması
        OrderResponseDto orderDto = new OrderResponseDto();
        orderDto.setId(1L);
        orderDto.setOrderNumber("ORD-1234567890");
        orderDto.setOrderDate(LocalDateTime.now());
        orderDto.setStatus(OrderStatus.GÖZLƏYİR);
        orderDto.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);
        orderDto.setPaymentMethod(PaymentMethod.NAĞD);
        orderDto.setTotalAmount(new BigDecimal("125.50"));

        OrderItemResponseDto itemDto = new OrderItemResponseDto();
        itemDto.setProductName("Əyləc diski");
        itemDto.setQuantity(2);
        orderDto.setItems(List.of(itemDto));

        when(orderService.getUserOrders(eq(15L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orderDto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/orders").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ORD-1234567890")))
                .andExpect(content().string(containsString("Əyləc diski")))
                .andExpect(content().string(containsString("125.50 AZN")))
                .andExpect(content().string(containsString("Gözləyir")))
                .andExpect(content().string(containsString("Ödəniş Gözləyən")));

        // 2. Boş siyahı görünüşü
        when(orderService.getUserOrders(eq(15L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/orders").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hələ sifarişiniz yoxdur")));
    }

    @Test
    @DisplayName("orders.html şablonunda sifariş nömrəsi /orders/{id} ətraflı baxış səhifəsinə keçid verir")
    void testOrdersTemplate_OrderNumberLinksToDetails() throws Exception {
        OrderResponseDto orderDto = new OrderResponseDto();
        orderDto.setId(42L);
        orderDto.setOrderNumber("ORD-424242");
        orderDto.setOrderDate(LocalDateTime.now());
        orderDto.setStatus(OrderStatus.HAZIRLANIR);
        orderDto.setPaymentStatus(PaymentStatus.TAM_ÖDƏNİLMİŞ);
        orderDto.setPaymentMethod(PaymentMethod.BANK_KÖÇÜRMƏSİ);
        orderDto.setTotalAmount(new BigDecimal("99.00"));

        when(orderService.getUserOrders(eq(15L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orderDto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/orders").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/orders/42\"")))
                .andExpect(content().string(containsString("#ORD-424242")));
    }

    @Test
    @DisplayName("GET /orders/{id} anonim istifadəçi üçün /login səhifəsinə yönləndirməlidir")
    void testOrderDetails_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/orders/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("GET /orders/{id} autentifikasiya olunmuş istifadəçi üçün 200 və order-details şablonu qaytarır")
    void testOrderDetails_Authenticated_Success() throws Exception {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(10L);
        dto.setOrderNumber("ORD-101010");
        dto.setOrderDate(LocalDateTime.now());
        dto.setStatus(OrderStatus.GÖZLƏYİR);
        dto.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);
        dto.setPaymentMethod(PaymentMethod.NAĞD);
        dto.setTotalAmount(new BigDecimal("100.00"));

        when(orderService.getOrderDetails(eq(15L), eq(10L))).thenReturn(dto);

        mockMvc.perform(get("/orders/10").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("order-details"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("order", dto));

        verify(orderService).getOrderDetails(eq(15L), eq(10L));
    }

    @Test
    @DisplayName("GET /orders/{id} URL parametrlərində göndərilən yad userId-ni nəzərə almır və yalnız Principal ID istifadə edir")
    void testOrderDetails_IgnoresForeignUserIdInUrl() throws Exception {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(10L);
        dto.setOrderNumber("ORD-101010");

        when(orderService.getOrderDetails(eq(15L), eq(10L))).thenReturn(dto);

        mockMvc.perform(get("/orders/10?userId=999").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("order-details"));

        verify(orderService).getOrderDetails(eq(15L), eq(10L));
        verify(orderService, never()).getOrderDetails(eq(999L), any());
    }

    @Test
    @DisplayName("GET /orders/{id} tapılmayan sifariş üçün həqiqi HTTP 404, error/404 və 'Sifariş tapılmadı!' mesajı qaytarır")
    void testOrderDetails_NotFound_Returns404() throws Exception {
        when(orderService.getOrderDetails(eq(15L), eq(9999L)))
                .thenThrow(new EntityNotFoundException("Sifariş tapılmadı!"));

        mockMvc.perform(get("/orders/9999").with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"))
                .andExpect(content().string(containsString("Sifariş tapılmadı!")));

        verify(orderService).getOrderDetails(eq(15L), eq(9999L));
    }

    @Test
    @DisplayName("GET /orders/{id} başqa istifadəçiyə aid sifariş üçün həqiqi HTTP 404, error/404 və 'Sifariş tapılmadı!' mesajı qaytarır")
    void testOrderDetails_BelongsToOtherUser_Returns404() throws Exception {
        when(orderService.getOrderDetails(eq(15L), eq(88L)))
                .thenThrow(new EntityNotFoundException("Sifariş tapılmadı!"));

        mockMvc.perform(get("/orders/88").with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"))
                .andExpect(content().string(containsString("Sifariş tapılmadı!")));

        verify(orderService).getOrderDetails(eq(15L), eq(88L));
    }

    @Test
    @DisplayName("GET /orders/{id} müsbət olmayan ID-lər (0 və mənfi) üçün həqiqi HTTP 404 qaytarır")
    void testOrderDetails_NonPositiveId_Returns404() throws Exception {
        // ID = 0
        mockMvc.perform(get("/orders/0").with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"))
                .andExpect(content().string(containsString("Sifariş tapılmadı!")));

        // ID = -5
        mockMvc.perform(get("/orders/-5").with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"))
                .andExpect(content().string(containsString("Sifariş tapılmadı!")));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("GET /orders/{id} yanlış formatlı ID (qeyri-rəqəm) üçün həqiqi HTTP 404 qaytarır")
    void testOrderDetails_InvalidFormatId_Returns404() throws Exception {
        mockMvc.perform(get("/orders/invalid-id").with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"))
                .andExpect(content().string(containsString("Sifariş tapılmadı!")));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("order-details.html şablonu bütün sahələri və məbləğləri DTO-dan düzgün render edir")
    void testOrderDetailsTemplate_Rendering_FullDetails() throws Exception {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(10L);
        dto.setOrderNumber("ORD-889900");
        dto.setOrderDate(LocalDateTime.of(2026, 10, 24, 15, 30));
        dto.setStatus(OrderStatus.KARQOYA_VERİLDİ);
        dto.setPaymentStatus(PaymentStatus.TAM_ÖDƏNİLMİŞ);
        dto.setPaymentMethod(PaymentMethod.BANK_KÖÇÜRMƏSİ);
        dto.setShippingAddress("Bakı ş., Nizami r., Heydər Əliyev pr. 105");
        dto.setTrackingNumber("TRK-AZ-998877");
        dto.setOrderNotes("Qapıda zəng edin, təcili çatdırılma.");
        dto.setTotalItems(3L);
        dto.setTotalAmount(new BigDecimal("190.50"));

        OrderItemResponseDto item1 = new OrderItemResponseDto();
        item1.setId(1L);
        item1.setProductId(101L);
        item1.setProductName("Əyləc bəndi");
        item1.setProductCode("BRK-101");
        item1.setImageUrl("/product-images/brake.jpg");
        item1.setQuantity(2);
        item1.setPriceAtPurchase(new BigDecimal("45.25"));
        item1.setSubtotal(new BigDecimal("90.50"));

        OrderItemResponseDto item2 = new OrderItemResponseDto();
        item2.setId(2L);
        item2.setProductId(102L);
        item2.setProductName("Yağ filtri");
        item2.setProductCode("FLT-202");
        item2.setImageUrl(null); // Şəkilsiz məhsul
        item2.setQuantity(1);
        item2.setPriceAtPurchase(new BigDecimal("100.00"));
        item2.setSubtotal(new BigDecimal("100.00"));

        dto.setItems(List.of(item1, item2));

        when(orderService.getOrderDetails(eq(15L), eq(10L))).thenReturn(dto);

        mockMvc.perform(get("/orders/10").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ORD-889900")))
                .andExpect(content().string(containsString("24.10.2026 15:30")))
                .andExpect(content().string(containsString("Karqoya verildi")))
                .andExpect(content().string(containsString("Tam Ödənilmiş")))
                .andExpect(content().string(containsString("Bank köçürməsi")))
                .andExpect(content().string(containsString("Bakı ş., Nizami r., Heydər Əliyev pr. 105")))
                .andExpect(content().string(containsString("TRK-AZ-998877")))
                .andExpect(content().string(containsString("Qapıda zəng edin, təcili çatdırılma.")))
                .andExpect(content().string(containsString("Əyləc bəndi")))
                .andExpect(content().string(containsString("BRK-101")))
                .andExpect(content().string(containsString("45.25 AZN")))
                .andExpect(content().string(containsString("90.50 AZN")))
                .andExpect(content().string(containsString("Yağ filtri")))
                .andExpect(content().string(containsString("FLT-202")))
                .andExpect(content().string(containsString("100.00 AZN")))
                .andExpect(content().string(containsString("190.50 AZN")))
                .andExpect(content().string(containsString("Sifarişlərimə qayıt")))
                .andExpect(content().string(containsString("href=\"/orders\"")));
    }

    @Test
    @DisplayName("order-details.html könüllü sahələr boş və şəkil olmadıqda səliqəli işləyir")
    void testOrderDetailsTemplate_Rendering_EmptyOptionalFields() throws Exception {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(11L);
        dto.setOrderNumber("ORD-EMPTY-01");
        dto.setOrderDate(LocalDateTime.now());
        dto.setStatus(OrderStatus.GÖZLƏYİR);
        dto.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);
        dto.setPaymentMethod(null);
        dto.setShippingAddress(null);
        dto.setTrackingNumber(null);
        dto.setOrderNotes(null);
        dto.setTotalItems(1L);
        dto.setTotalAmount(new BigDecimal("50.00"));

        OrderItemResponseDto item = new OrderItemResponseDto();
        item.setId(5L);
        item.setProductName("Qeyri-aktiv köhnə detal");
        item.setProductCode(null);
        item.setImageUrl(null);
        item.setQuantity(1);
        item.setPriceAtPurchase(new BigDecimal("50.00"));
        item.setSubtotal(new BigDecimal("50.00"));

        dto.setItems(List.of(item));

        when(orderService.getOrderDetails(eq(15L), eq(11L))).thenReturn(dto);

        mockMvc.perform(get("/orders/11").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ORD-EMPTY-01")))
                .andExpect(content().string(containsString("Qeyri-aktiv köhnə detal")))
                .andExpect(content().string(containsString("50.00 AZN")));
    }

    // ==========================================
    // POST /orders/{id}/cancel TESTLƏRİ
    // ==========================================

    @Test
    @DisplayName("POST /orders/{id}/cancel anonim istifadəçi üçün /login səhifəsinə yönləndirməlidir")
    void testCancelOrder_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/orders/10/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("POST /orders/{id}/cancel CSRF tokensiz sorğuda 403 Forbidden qaytarmalı və servisi çağırmamalıdır")
    void testCancelOrder_WithoutCsrf_Returns403() throws Exception {
        mockMvc.perform(post("/orders/10/cancel").with(user(userDetails)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("POST /orders/{id}/cancel uğurla tamamlandıqda /orders ünvanına yönləndirməli və flash mesajı göstərməlidir")
    void testCancelOrder_Success_RedirectsToOrdersWithFlashMessage() throws Exception {
        doNothing().when(orderService).cancelOrder(eq(15L), eq(10L));

        mockMvc.perform(post("/orders/10/cancel").with(user(userDetails)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attribute("successMessage", "Sifariş ləğv edildi."));

        verify(orderService).cancelOrder(eq(15L), eq(10L));
    }

    @Test
    @DisplayName("POST /orders/{id}/cancel URL və form parametrlərindəki yad userId-ni qəbul etmir və yalnız Principal ID istifadə edir")
    void testCancelOrder_IgnoresForeignUserIdInUrlAndForm() throws Exception {
        doNothing().when(orderService).cancelOrder(eq(15L), eq(10L));

        mockMvc.perform(post("/orders/10/cancel?userId=999")
                        .param("userId", "888")
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attribute("successMessage", "Sifariş ləğv edildi."));

        verify(orderService).cancelOrder(eq(15L), eq(10L));
        verify(orderService, never()).cancelOrder(eq(999L), any());
        verify(orderService, never()).cancelOrder(eq(888L), any());
    }

    @Test
    @DisplayName("POST /orders/{id}/cancel status/ödəniş məhdudiyyəti kimi IllegalStateException hallarında /orders-ə yönləndirməli və errorMessage göstərməlidir")
    void testCancelOrder_BusinessRejection_IllegalStateException_RedirectsWithErrorMessage() throws Exception {
        doThrow(new IllegalStateException("Yalnız 'Gözləyir' statusunda olan sifarişlər ləğv edilə bilər!"))
                .when(orderService).cancelOrder(eq(15L), eq(10L));

        mockMvc.perform(post("/orders/10/cancel").with(user(userDetails)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attribute("errorMessage", "Yalnız 'Gözləyir' statusunda olan sifarişlər ləğv edilə bilər!"));

        verify(orderService).cancelOrder(eq(15L), eq(10L));
    }

    @Test
    @DisplayName("POST /orders/{id}/cancel tapılmayan və ya başqasına aid sifariş üçün həqiqi HTTP 404, error/404 və 'Sifariş tapılmadı!' qaytarmalıdır")
    void testCancelOrder_OrderNotFoundOrBelongsToOtherUser_Returns404() throws Exception {
        doThrow(new EntityNotFoundException("Sifariş tapılmadı!"))
                .when(orderService).cancelOrder(eq(15L), eq(99L));

        mockMvc.perform(post("/orders/99/cancel").with(user(userDetails)).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"))
                .andExpect(content().string(containsString("Sifariş tapılmadı!")));

        verify(orderService).cancelOrder(eq(15L), eq(99L));
    }

    @Test
    @DisplayName("POST /orders/{id}/cancel müsbət olmayan ID-lər (0 və mənfi) üçün həqiqi HTTP 404 qaytarmalıdır")
    void testCancelOrder_NonPositiveId_Returns404() throws Exception {
        mockMvc.perform(post("/orders/0/cancel").with(user(userDetails)).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"))
                .andExpect(content().string(containsString("Sifariş tapılmadı!")));

        mockMvc.perform(post("/orders/-5/cancel").with(user(userDetails)).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"))
                .andExpect(content().string(containsString("Sifariş tapılmadı!")));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("POST /orders/{id}/cancel yanlış formatlı ID üçün həqiqi HTTP 404 qaytarmalıdır")
    void testCancelOrder_InvalidFormatId_Returns404() throws Exception {
        mockMvc.perform(post("/orders/invalid-id/cancel").with(user(userDetails)).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"))
                .andExpect(content().string(containsString("Sifariş tapılmadı!")));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("GET /orders/{id}/cancel marşrutu servisi çağırmamalıdır")
    void testCancelOrder_GetRequest_DoesNotCallService() throws Exception {
        mockMvc.perform(get("/orders/10/cancel").with(user(userDetails)))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("orders.html şablonunda canCancel=true olduqda 'Ləğv et' forması render edilir")
    void testOrdersTemplate_CanCancelTrue_RendersCancelButton() throws Exception {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(10L);
        dto.setOrderNumber("ORD-CANCEL-01");
        dto.setOrderDate(LocalDateTime.now());
        dto.setStatus(OrderStatus.GÖZLƏYİR);
        dto.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);
        dto.setTotalAmount(new BigDecimal("100.00"));
        dto.setCanCancel(true);

        when(orderService.getUserOrders(eq(15L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/orders").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/orders/10/cancel\"")))
                .andExpect(content().string(containsString("Ləğv et")));
    }

    @Test
    @DisplayName("orders.html şablonunda canCancel=false olduqda 'Ləğv et' forması render edilmir")
    void testOrdersTemplate_CanCancelFalse_DoesNotRenderCancelButton() throws Exception {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(10L);
        dto.setOrderNumber("ORD-CANCEL-02");
        dto.setOrderDate(LocalDateTime.now());
        dto.setStatus(OrderStatus.LƏĞV_EDİLDİ);
        dto.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);
        dto.setTotalAmount(new BigDecimal("100.00"));
        dto.setCanCancel(false);

        when(orderService.getUserOrders(eq(15L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/orders").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ləğv edildi")))
                .andExpect(content().string(not(containsString("action=\"/orders/10/cancel\""))));
    }

    @Test
    @DisplayName("order-details.html şablonunda canCancel=true olduqda 'Ləğv et' forması render edilir")
    void testOrderDetailsTemplate_CanCancelTrue_RendersCancelButton() throws Exception {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(10L);
        dto.setOrderNumber("ORD-CANCEL-03");
        dto.setOrderDate(LocalDateTime.now());
        dto.setStatus(OrderStatus.GÖZLƏYİR);
        dto.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);
        dto.setTotalAmount(new BigDecimal("100.00"));
        dto.setCanCancel(true);

        when(orderService.getOrderDetails(eq(15L), eq(10L))).thenReturn(dto);

        mockMvc.perform(get("/orders/10").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/orders/10/cancel\"")))
                .andExpect(content().string(containsString("Ləğv et")));
    }

    @Test
    @DisplayName("order-details.html şablonunda canCancel=false olduqda 'Ləğv et' forması render edilmir")
    void testOrderDetailsTemplate_CanCancelFalse_DoesNotRenderCancelButton() throws Exception {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(10L);
        dto.setOrderNumber("ORD-CANCEL-04");
        dto.setOrderDate(LocalDateTime.now());
        dto.setStatus(OrderStatus.HAZIRLANIR);
        dto.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);
        dto.setTotalAmount(new BigDecimal("100.00"));
        dto.setCanCancel(false);

        when(orderService.getOrderDetails(eq(15L), eq(10L))).thenReturn(dto);

        mockMvc.perform(get("/orders/10").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hazırlanır")))
                .andExpect(content().string(not(containsString("action=\"/orders/10/cancel\""))));
    }

    @Configuration
    @EnableWebMvc
    @EnableSpringDataWebSupport
    static class TestConfig {

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        public OrderService orderService() {
            return mock(OrderService.class);
        }

        @Bean
        public com.celiloglu.backend.service.PaymentService paymentService() {
            com.celiloglu.backend.service.PaymentService mock = mock(com.celiloglu.backend.service.PaymentService.class);
            when(mock.getCustomerPaymentsByOrderId(any())).thenReturn(Collections.emptyList());
            when(mock.getPaidAmountByOrderId(any())).thenReturn(BigDecimal.ZERO);
            return mock;
        }

        @Bean
        public OrderController orderController(OrderService orderService, com.celiloglu.backend.service.PaymentService paymentService) {
            return new OrderController(orderService, paymentService);
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
