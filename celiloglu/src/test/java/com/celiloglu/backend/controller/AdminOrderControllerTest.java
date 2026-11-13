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
import com.celiloglu.backend.service.OrderExcelExportService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
                com.celiloglu.backend.security.RoleBasedAuthenticationSuccessHandler.class,
                com.celiloglu.backend.controller.SecurityModelAdvice.class,
                AdminOrderControllerTest.TestConfig.class
})
@WebAppConfiguration
class AdminOrderControllerTest {

        @Autowired
        private WebApplicationContext context;

        @Autowired
        private OrderService orderService;

        private MockMvc mockMvc;
        private CustomUserDetails adminUserDetails;
        private CustomUserDetails superAdminUserDetails;
        private CustomUserDetails regularUserDetails;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();

                reset(orderService);

                adminUserDetails = new CustomUserDetails(
                                1L,
                                "admin_user",
                                "Password123!",
                                true,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

                superAdminUserDetails = new CustomUserDetails(
                                2L,
                                "super_admin_user",
                                "Password123!",
                                true,
                                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

                regularUserDetails = new CustomUserDetails(
                                3L,
                                "regular_user",
                                "Password123!",
                                true,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        }

        @Test
        @DisplayName("GET /admin/orders anonim istifadəçi üçün /login səhifəsinə yönləndirilməlidir")
        void testListOrders_Unauthenticated_RedirectsToLogin() throws Exception {
                mockMvc.perform(get("/admin/orders"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/login"));

                verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("GET /admin/orders ROLE_USER üçün 403 Forbidden qaytarmalıdır")
        void testListOrders_RoleUser_Forbidden() throws Exception {
                mockMvc.perform(get("/admin/orders").with(user(regularUserDetails)))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("GET /admin/orders ROLE_ADMIN üçün uğurla 200 və admin/orders şablonunu qaytarır")
        void testListOrders_RoleAdmin_Success() throws Exception {
                OrderResponseDto order = createSampleOrder(101L, "ORD-101", 10L, "customer1", "Müştəri Bir",
                                "Şirkət A");
                Page<OrderResponseDto> page = new PageImpl<>(List.of(order));
                when(orderService.getAllOrdersForAdmin(any(Pageable.class))).thenReturn(page);

                mockMvc.perform(get("/admin/orders").with(user(adminUserDetails)))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/orders"))
                                .andExpect(model().attributeExists("ordersPage", "orders"))
                                .andExpect(content().string(containsString("ORD-101")))
                                .andExpect(content().string(containsString("Müştəri Bir")))
                                .andExpect(content().string(containsString("Şirkət A")));

                verify(orderService, times(1)).getAllOrdersForAdmin(any(Pageable.class));
        }

        @Test
        @DisplayName("GET /admin/orders ROLE_SUPER_ADMIN üçün uğurla 200 qaytarır")
        void testListOrders_RoleSuperAdmin_Success() throws Exception {
                when(orderService.getAllOrdersForAdmin(any(Pageable.class))).thenReturn(Page.empty());

                mockMvc.perform(get("/admin/orders").with(user(superAdminUserDetails)))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/orders"))
                                .andExpect(model().attributeExists("ordersPage", "orders"));
        }

        @Test
        @DisplayName("GET /admin/orders.html mappingi də işləkdir")
        void testListOrdersHtmlMapping_Success() throws Exception {
                when(orderService.getAllOrdersForAdmin(any(Pageable.class))).thenReturn(Page.empty());

                mockMvc.perform(get("/admin/orders.html").with(user(adminUserDetails)))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/orders"));
        }

        @Test
        @DisplayName("Admin sifariş siyahısında fərqli müştərilərin sifarişlərini görür (admin userId ilə filtrlənmir)")
        void testListOrders_MultipleCustomers_RenderedTogether() throws Exception {
                OrderResponseDto order1 = createSampleOrder(101L, "ORD-101", 10L, "customer1", "Müştəri Bir",
                                "Şirkət A");
                OrderResponseDto order2 = createSampleOrder(102L, "ORD-102", 20L, "customer2", "Müştəri İki",
                                "Şirkət B");

                Page<OrderResponseDto> page = new PageImpl<>(List.of(order1, order2));
                when(orderService.getAllOrdersForAdmin(any(Pageable.class))).thenReturn(page);

                mockMvc.perform(get("/admin/orders").with(user(adminUserDetails)))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("ORD-101")))
                                .andExpect(content().string(containsString("ORD-102")))
                                .andExpect(content().string(containsString("Müştəri Bir")))
                                .andExpect(content().string(containsString("Müştəri İki")));
        }

        @Test
        @DisplayName("Səhifələmə parametrləri (page=2, size=5) servisə düzgün ötürülür")
        void testListOrders_PaginationParametersPassed() throws Exception {
                when(orderService.getAllOrdersForAdmin(any(Pageable.class))).thenReturn(Page.empty());

                mockMvc.perform(get("/admin/orders")
                                .param("page", "2")
                                .param("size", "5")
                                .with(user(adminUserDetails)))
                                .andExpect(status().isOk());

                ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
                verify(orderService).getAllOrdersForAdmin(captor.capture());
                assertEquals(2, captor.getValue().getPageNumber());
                assertEquals(5, captor.getValue().getPageSize());
        }

        @Test
        @DisplayName("Mənfi səhifə və ya sıfır ölçü gəldikdə defolt dəyərlər tətbiq olunur")
        void testListOrders_NegativePagination_FallbackToDefaults() throws Exception {
                when(orderService.getAllOrdersForAdmin(any(Pageable.class))).thenReturn(Page.empty());

                mockMvc.perform(get("/admin/orders")
                                .param("page", "-3")
                                .param("size", "0")
                                .with(user(adminUserDetails)))
                                .andExpect(status().isOk());

                ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
                verify(orderService).getAllOrdersForAdmin(captor.capture());
                assertEquals(0, captor.getValue().getPageNumber());
                assertEquals(10, captor.getValue().getPageSize());
        }

        @Test
        @DisplayName("Sifarişlər siyahısı boş olduqda boş vəziyyət mesajı səliqəli göstərilir")
        void testListOrders_EmptyPage_RendersEmptyState() throws Exception {
                when(orderService.getAllOrdersForAdmin(any(Pageable.class)))
                                .thenReturn(new PageImpl<>(Collections.emptyList()));

                mockMvc.perform(get("/admin/orders").with(user(adminUserDetails)))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("Heç bir sifariş tapılmadı")));
        }

        @Test
        @DisplayName("GET /admin/orders/{id} anonim istifadəçi üçün /login səhifəsinə yönləndirir")
        void testOrderDetails_Unauthenticated_RedirectsToLogin() throws Exception {
                mockMvc.perform(get("/admin/orders/101"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/login"));

                verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("GET /admin/orders/{id} ROLE_USER üçün 403 Forbidden qaytarır")
        void testOrderDetails_RoleUser_Forbidden() throws Exception {
                mockMvc.perform(get("/admin/orders/101").with(user(regularUserDetails)))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("GET /admin/orders/{id} ROLE_ADMIN üçün uğurla 200 və admin/order-details şablonunu qaytarır")
        void testOrderDetails_RoleAdmin_Success() throws Exception {
                OrderResponseDto dto = createSampleOrder(101L, "ORD-101", 10L, "customer1", "Müştəri Bir", "Şirkət A");
                dto.setShippingAddress("Bakı ş., Nizami ray. 42");
                dto.setOrderNotes("Təcili çatdırılma xahiş olunur");
                dto.setTrackingNumber("TRK-123456");

                OrderItemResponseDto item = new OrderItemResponseDto(
                                1L, 50L, "Əyləc Bəndi", "BRK-001", "/images/brake.jpg",
                                2, new BigDecimal("45.00"), new BigDecimal("90.00"));
                dto.setItems(List.of(item));

                when(orderService.getOrderDetailsForAdmin(101L)).thenReturn(dto);

                mockMvc.perform(get("/admin/orders/101").with(user(adminUserDetails)))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/order-details"))
                                .andExpect(model().attributeExists("order"))
                                .andExpect(content().string(containsString("ORD-101")))
                                .andExpect(content().string(containsString("Bakı ş., Nizami ray. 42")))
                                .andExpect(content().string(containsString("Təcili çatdırılma xahiş olunur")))
                                .andExpect(content().string(containsString("TRK-123456")))
                                .andExpect(content().string(containsString("Əyləc Bəndi")))
                                .andExpect(content().string(containsString("BRK-001")))
                                .andExpect(content().string(containsString("45.00")))
                                .andExpect(content().string(containsString("90.00")));

                verify(orderService, times(1)).getOrderDetailsForAdmin(101L);
        }

        @Test
        @DisplayName("GET /admin/orders/{id} ROLE_SUPER_ADMIN üçün uğurla 200 qaytarır")
        void testOrderDetails_RoleSuperAdmin_Success() throws Exception {
                OrderResponseDto dto = createSampleOrder(101L, "ORD-101", 10L, "customer1", "Müştəri Bir", "Şirkət A");
                when(orderService.getOrderDetailsForAdmin(101L)).thenReturn(dto);

                mockMvc.perform(get("/admin/orders/101").with(user(superAdminUserDetails)))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/order-details"));
        }

        @Test
        @DisplayName("Admin sifariş detallarında qeyri-aktiv edilmiş məhsulu da gizlətmir və tarixi alış qiymətini qoruyur")
        void testOrderDetails_InactiveProductAndHistoricPricePreserved() throws Exception {
                OrderResponseDto dto = createSampleOrder(102L, "ORD-102", 11L, "customer2", "Müştəri İki", null);
                OrderItemResponseDto inactiveItem = new OrderItemResponseDto(
                                2L, 51L, "Köhnə Filter", "FLT-OLD", "/images/old-filter.jpg",
                                3, new BigDecimal("12.50"), new BigDecimal("37.50"));
                dto.setItems(List.of(inactiveItem));

                when(orderService.getOrderDetailsForAdmin(102L)).thenReturn(dto);

                mockMvc.perform(get("/admin/orders/102").with(user(adminUserDetails)))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("Köhnə Filter")))
                                .andExpect(content().string(containsString("FLT-OLD")))
                                .andExpect(content().string(containsString("12.50")))
                                .andExpect(content().string(containsString("37.50")));
        }

        @Test
        @DisplayName("Mövcud olmayan sifariş ID-si verildikdə HTTP 404 və error/404 şablonu qayıdır")
        void testOrderDetails_NotFound_Returns404() throws Exception {
                when(orderService.getOrderDetailsForAdmin(999L))
                                .thenThrow(new EntityNotFoundException("Sifariş tapılmadı!"));

                mockMvc.perform(get("/admin/orders/999").with(user(adminUserDetails)))
                                .andExpect(status().isNotFound())
                                .andExpect(view().name("error/404"))
                                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"))
                                .andExpect(content().string(containsString("Sifariş tapılmadı!")));
        }

        @Test
        @DisplayName("Sıfır və ya mənfi sifariş ID-si verildikdə HTTP 404 və error/404 şablonu qayıdır")
        void testOrderDetails_NonPositiveId_Returns404() throws Exception {
                mockMvc.perform(get("/admin/orders/0").with(user(adminUserDetails)))
                                .andExpect(status().isNotFound())
                                .andExpect(view().name("error/404"))
                                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"));

                mockMvc.perform(get("/admin/orders/-5").with(user(adminUserDetails)))
                                .andExpect(status().isNotFound())
                                .andExpect(view().name("error/404"))
                                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"));

                verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("Formatı yanlış olan sifariş ID-si (məs: /admin/orders/abc) verildikdə HTTP 404 və error/404 şablonu qayıdır")
        void testOrderDetails_InvalidFormatId_Returns404() throws Exception {
                mockMvc.perform(get("/admin/orders/abc").with(user(adminUserDetails)))
                                .andExpect(status().isNotFound())
                                .andExpect(view().name("error/404"))
                                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"));

                verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("GET sorğuları heç bir yazma və ya ləğv əməliyyatı çağırmır (read-only)")
        void testGetRequestsDoNotPerformWriteOperations() throws Exception {
                when(orderService.getAllOrdersForAdmin(any(Pageable.class))).thenReturn(Page.empty());

                mockMvc.perform(get("/admin/orders").with(user(adminUserDetails)))
                                .andExpect(status().isOk());

                verify(orderService, times(1)).getAllOrdersForAdmin(any(Pageable.class));
                verify(orderService, never()).cancelOrder(anyLong(), anyLong());
                verify(orderService, never()).createOrderFromCart(anyLong(), any(), any());
        }

        @Test
        @DisplayName("POST /admin/orders/{id}/status anonim istifadəçi üçün /login səhifəsinə yönləndirir")
        void testUpdateOrderStatus_Unauthenticated_RedirectsToLogin() throws Exception {
                mockMvc.perform(post("/admin/orders/101/status")
                                .with(csrf())
                                .param("targetStatus", "HAZIRLANIR"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/login"));

                verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("POST /admin/orders/{id}/status ROLE_USER üçün 403 Forbidden qaytarır")
        void testUpdateOrderStatus_RoleUser_Forbidden() throws Exception {
                mockMvc.perform(post("/admin/orders/101/status")
                                .with(user(regularUserDetails))
                                .with(csrf())
                                .param("targetStatus", "HAZIRLANIR"))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("POST /admin/orders/{id}/status CSRF tokensiz göndərildikdə 403 Forbidden qaytarır")
        void testUpdateOrderStatus_WithoutCsrf_Forbidden() throws Exception {
                mockMvc.perform(post("/admin/orders/101/status")
                                .with(user(adminUserDetails))
                                .param("targetStatus", "HAZIRLANIR"))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("POST /admin/orders/{id}/status ROLE_ADMIN və CSRF ilə uğurla statusu yeniləyir və detallara redirect edir")
        void testUpdateOrderStatus_RoleAdmin_Success() throws Exception {
                mockMvc.perform(post("/admin/orders/101/status")
                                .with(user(adminUserDetails))
                                .with(csrf())
                                .param("targetStatus", "HAZIRLANIR"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/orders/101"))
                                .andExpect(flash().attribute("successMessage", "Sifarişin statusu uğurla yeniləndi."));

                verify(orderService, times(1)).updateOrderStatusForAdmin(101L, "HAZIRLANIR");
        }

        @Test
        @DisplayName("POST /admin/orders/{id}/status ROLE_SUPER_ADMIN və CSRF ilə uğurla işləyir")
        void testUpdateOrderStatus_RoleSuperAdmin_Success() throws Exception {
                mockMvc.perform(post("/admin/orders/102/status")
                                .with(user(superAdminUserDetails))
                                .with(csrf())
                                .param("targetStatus", "KARQOYA_VERİLDİ"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/orders/102"))
                                .andExpect(flash().attribute("successMessage", "Sifarişin statusu uğurla yeniləndi."));

                verify(orderService, times(1)).updateOrderStatusForAdmin(102L, "KARQOYA_VERİLDİ");
        }

        @Test
        @DisplayName("POST /admin/orders/{id}/status olmayan sifariş ID-si üçün HTTP 404 qaytarır")
        void testUpdateOrderStatus_NotFound_Returns404() throws Exception {
                doThrow(new EntityNotFoundException("Sifariş tapılmadı!"))
                                .when(orderService).updateOrderStatusForAdmin(999L, "HAZIRLANIR");

                mockMvc.perform(post("/admin/orders/999/status")
                                .with(user(adminUserDetails))
                                .with(csrf())
                                .param("targetStatus", "HAZIRLANIR"))
                                .andExpect(status().isNotFound())
                                .andExpect(view().name("error/404"))
                                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"));
        }

        @Test
        @DisplayName("POST /admin/orders/{id}/status mənfi və ya sıfır ID üçün HTTP 404 qaytarır")
        void testUpdateOrderStatus_NonPositiveId_Returns404() throws Exception {
                mockMvc.perform(post("/admin/orders/0/status")
                                .with(user(adminUserDetails))
                                .with(csrf())
                                .param("targetStatus", "HAZIRLANIR"))
                                .andExpect(status().isNotFound())
                                .andExpect(view().name("error/404"))
                                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"));

                verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("POST /admin/orders/{id}/status formatı yanlış ID (məs: /status/abc) üçün HTTP 404 qaytarır")
        void testUpdateOrderStatus_InvalidFormatId_Returns404() throws Exception {
                mockMvc.perform(post("/admin/orders/invalid/status")
                                .with(user(adminUserDetails))
                                .with(csrf())
                                .param("targetStatus", "HAZIRLANIR"))
                                .andExpect(status().isNotFound())
                                .andExpect(view().name("error/404"))
                                .andExpect(model().attribute("errorMessage", "Sifariş tapılmadı!"));

                verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("POST /admin/orders/{id}/status biznes xətalarında (məs: ləğv edilmiş sifariş, atlanan mərhələ) detallara redirect və errorMessage flash mesajı verir")
        void testUpdateOrderStatus_BusinessError_RedirectsWithFlashError() throws Exception {
                doThrow(new IllegalStateException("Ləğv edilmiş sifarişin statusu dəyişdirilə bilməz!"))
                                .when(orderService).updateOrderStatusForAdmin(103L, "HAZIRLANIR");

                mockMvc.perform(post("/admin/orders/103/status")
                                .with(user(adminUserDetails))
                                .with(csrf())
                                .param("targetStatus", "HAZIRLANIR"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/orders/103"))
                                .andExpect(flash().attribute("errorMessage",
                                                "Ləğv edilmiş sifarişin statusu dəyişdirilə bilməz!"));
        }

        @Test
        @DisplayName("order-details.html: Status GÖZLƏYİR olduqda yalnız 'Təsdiqlə' forması göstərilir")
        void testOrderDetailsTemplate_StatusGozleyir_ShowsConfirmButton() throws Exception {
                OrderResponseDto dto = createSampleOrder(10L, "ORD-BTN-1", 1L, "u1", "İstifadəçi 1", null);
                dto.setStatus(OrderStatus.GÖZLƏYİR);
                when(orderService.getOrderDetailsForAdmin(10L)).thenReturn(dto);

                mockMvc.perform(get("/admin/orders/10").with(user(adminUserDetails)))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("action=\"/admin/orders/10/status\"")))
                                .andExpect(content().string(containsString("value=\"HAZIRLANIR\"")))
                                .andExpect(content().string(containsString("Təsdiqlə")))
                                .andExpect(content().string(not(containsString("value=\"KARQOYA_VERİLDİ\""))))
                                .andExpect(content().string(not(containsString("value=\"ÇATDIRILDI\""))));
        }

        @Test
        @DisplayName("order-details.html: Status HAZIRLANIR olduqda yalnız 'Karqoya ver' forması göstərilir")
        void testOrderDetailsTemplate_StatusHazirlanir_ShowsShipButton() throws Exception {
                OrderResponseDto dto = createSampleOrder(20L, "ORD-BTN-2", 1L, "u1", "İstifadəçi 1", null);
                dto.setStatus(OrderStatus.HAZIRLANIR);
                when(orderService.getOrderDetailsForAdmin(20L)).thenReturn(dto);

                mockMvc.perform(get("/admin/orders/20").with(user(adminUserDetails)))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("action=\"/admin/orders/20/status\"")))
                                .andExpect(content().string(containsString("value=\"KARQOYA_VERİLDİ\"")))
                                .andExpect(content().string(containsString("Karqoya ver")))
                                .andExpect(content().string(not(containsString("value=\"HAZIRLANIR\""))))
                                .andExpect(content().string(not(containsString("value=\"ÇATDIRILDI\""))));
        }

        @Test
        @DisplayName("order-details.html: Status KARQOYA_VERİLDİ olduqda yalnız 'Çatdırıldı kimi qeyd et' forması göstərilir")
        void testOrderDetailsTemplate_StatusKarqoyaVerildi_ShowsDeliverButton() throws Exception {
                OrderResponseDto dto = createSampleOrder(30L, "ORD-BTN-3", 1L, "u1", "İstifadəçi 1", null);
                dto.setStatus(OrderStatus.KARQOYA_VERİLDİ);
                when(orderService.getOrderDetailsForAdmin(30L)).thenReturn(dto);

                mockMvc.perform(get("/admin/orders/30").with(user(adminUserDetails)))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("action=\"/admin/orders/30/status\"")))
                                .andExpect(content().string(containsString("value=\"ÇATDIRILDI\"")))
                                .andExpect(content().string(containsString("Çatdırıldı kimi qeyd et")))
                                .andExpect(content().string(not(containsString("value=\"HAZIRLANIR\""))))
                                .andExpect(content().string(not(containsString("value=\"KARQOYA_VERİLDİ\""))));
        }

        @Test
        @DisplayName("order-details.html: Status ÇATDIRILDI və ya LƏĞV_EDİLDİ olduqda heç bir düymə göstərilmir")
        void testOrderDetailsTemplate_FinalStatuses_NoButtonsShown() throws Exception {
                OrderResponseDto dtoDelivered = createSampleOrder(40L, "ORD-BTN-4", 1L, "u1", "İstifadəçi 1", null);
                dtoDelivered.setStatus(OrderStatus.ÇATDIRILDI);
                when(orderService.getOrderDetailsForAdmin(40L)).thenReturn(dtoDelivered);

                mockMvc.perform(get("/admin/orders/40").with(user(adminUserDetails)))
                                .andExpect(status().isOk())
                                .andExpect(content().string(not(containsString("action=\"/admin/orders/40/status\""))));

                OrderResponseDto dtoCancelled = createSampleOrder(50L, "ORD-BTN-5", 1L, "u1", "İstifadəçi 1", null);
                dtoCancelled.setStatus(OrderStatus.LƏĞV_EDİLDİ);
                when(orderService.getOrderDetailsForAdmin(50L)).thenReturn(dtoCancelled);

                mockMvc.perform(get("/admin/orders/50").with(user(adminUserDetails)))
                                .andExpect(status().isOk())
                                .andExpect(content().string(not(containsString("action=\"/admin/orders/50/status\""))));
        }

        private OrderResponseDto createSampleOrder(Long id, String orderNumber, Long customerId,
                        String username, String fullName, String company) {
                OrderResponseDto dto = new OrderResponseDto();
                dto.setId(id);
                dto.setOrderNumber(orderNumber);
                dto.setOrderDate(LocalDateTime.now());
                dto.setStatus(OrderStatus.GÖZLƏYİR);
                dto.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);
                dto.setPaymentMethod(PaymentMethod.NAĞD);
                dto.setTotalAmount(new BigDecimal("120.00"));
                dto.setTotalItems(2L);
                dto.setCustomerId(customerId);
                dto.setCustomerUsername(username);
                dto.setCustomerFullName(fullName);
                dto.setCustomerEmail(username + "@example.com");
                dto.setCustomerPhone("+994501234567");
                dto.setCustomerCompanyName(company);
                dto.setCustomerTin(company != null ? "1234567890" : null);
                dto.setItems(Collections.emptyList());
                return dto;
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
                public com.celiloglu.backend.service.CartService cartService() {
                        return mock(com.celiloglu.backend.service.CartService.class);
                }

                @Bean
                public OrderService orderService() {
                        return mock(OrderService.class);
                }

                @Bean
                public com.celiloglu.backend.service.PaymentService paymentService() {
                        com.celiloglu.backend.service.PaymentService mock = mock(
                                        com.celiloglu.backend.service.PaymentService.class);
                        when(mock.generateReceiptNumber()).thenReturn("RCP-TEST123456");
                        return mock;
                }

                @Bean
                public OrderExcelExportService orderExcelExportService() {
                        return mock(OrderExcelExportService.class);
                }

                @Bean
                public AdminOrderController adminOrderController(OrderService orderService,
                                com.celiloglu.backend.service.PaymentService paymentService,
                                OrderExcelExportService orderExcelExportService) {
                        return new AdminOrderController(orderService, paymentService, orderExcelExportService);
                }

                @Bean
                public ViewResolver viewResolver() {
                        org.thymeleaf.templateresolver.ClassLoaderTemplateResolver templateResolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
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
