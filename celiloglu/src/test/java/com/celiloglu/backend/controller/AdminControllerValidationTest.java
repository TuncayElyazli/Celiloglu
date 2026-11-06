package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.request.ProductRequestDto;
import com.celiloglu.backend.dto.response.AdminDashboardDto;
import com.celiloglu.backend.dto.response.ProductResponseDto;
import com.celiloglu.backend.service.AdminDashboardService;
import com.celiloglu.backend.service.ProductFormOptionsService;
import com.celiloglu.backend.service.ProductImageStorageService;
import com.celiloglu.backend.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerValidationTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @Mock
    private ProductFormOptionsService optionsService;

    @Mock
    private ProductImageStorageService imageStorageService;

    @Mock
    private AdminDashboardService dashboardService;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        when(dashboardService.getDashboardData()).thenReturn(new AdminDashboardDto());
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    @Test
    @DisplayName("GET /admin/products/new açıldıqda miqdar və qiymət 0 olmur, null/boş qalır")
    void testNewProduct_FormDoesNotHaveZero() throws Exception {
        when(productService.getAllProducts(any())).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(optionsService.getCategories()).thenReturn(Collections.emptyList());
        when(optionsService.getCarBrands()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/products/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attribute("editingProductId", nullValue()))
                .andExpect(model().attribute("openProductModal", true))
                .andExpect(model().attribute("productForm", hasProperty("stockQuantity", nullValue())))
                .andExpect(model().attribute("productForm", hasProperty("price", nullValue())));
    }

    @Test
    @DisplayName("POST /admin/products xəta baş verdikdə HTTP 500 yaranmır, status 200 və admin şablonu qayıdır")
    void testCreateProduct_ValidationError_Returns200AndPreservesData() throws Exception {
        when(productService.getAllProducts(any())).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(optionsService.getCategories()).thenReturn(List.of(new ProductFormOptionsService.Choice(1L, "Filtrlər")));
        when(optionsService.getCarBrands()).thenReturn(Collections.emptyList());

        when(productService.createProduct(any(ProductRequestDto.class)))
                .thenThrow(new IllegalArgumentException("Kateqoriya seçilməlidir!"));

        mockMvc.perform(post("/admin/products")
                        .param("name", "Test Məhsul")
                        .param("code", "TST-01")
                        .param("price", "15.00")
                        .param("stockQuantity", "5")
                        .param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attribute("openProductModal", true))
                .andExpect(model().attributeHasErrors("productForm"))
                .andExpect(model().attribute("selectedCategoryName", is("Filtrlər")));
    }

    @Test
    @DisplayName("POST /admin/products/{id} redaktə zamanı xəta olduqda redaktə ID-si və dəyərlər saxlanılır")
    void testUpdateProduct_ValidationError_PreservesEditingId() throws Exception {
        when(productService.getAllProducts(any())).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(optionsService.getCategories()).thenReturn(Collections.emptyList());
        when(optionsService.getCarBrands()).thenReturn(Collections.emptyList());

        when(productService.updateProduct(any(), any()))
                .thenThrow(new IllegalArgumentException("Qiymət minimum 0.01 AZN olmalıdır!"));

        mockMvc.perform(post("/admin/products/42")
                        .param("name", "Mövcud Məhsul")
                        .param("code", "EX-42")
                        .param("price", "0.00")
                        .param("stockQuantity", "2")
                        .param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attribute("editingProductId", 42L))
                .andExpect(model().attribute("openProductModal", true))
                .andExpect(model().attributeHasErrors("productForm"));
    }

    @Test
    @DisplayName("GET /admin?tab=orders daxil olduqda /admin/orders ünvanına redirect edir")
    void testAdminOrdersTab_RedirectsToAdminOrders() throws Exception {
        mockMvc.perform(get("/admin").param("tab", "orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders"));
    }
}
