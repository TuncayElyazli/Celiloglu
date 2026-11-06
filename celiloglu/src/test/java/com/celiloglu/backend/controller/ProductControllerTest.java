package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.response.ProductResponseDto;
import com.celiloglu.backend.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

        private MockMvc mockMvc;

        @Mock
        private ProductService productService;

        @InjectMocks
        private ProductController productController;

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

                mockMvc = MockMvcBuilders.standaloneSetup(productController)
                                .setViewResolvers(viewResolver)
                                .build();
        }

        @Test
        @DisplayName("GET /products/{id} aktiv məhsul üçün 200 və product-details şablonu qaytarır")
        void testGetProductDetails_ActiveProduct_Success() throws Exception {
                ProductResponseDto dto = new ProductResponseDto();
                dto.setId(1L);
                dto.setName("Əyləc bəndi");
                dto.setCode("BRK-101");
                dto.setPrice(new BigDecimal("45.00"));
                dto.setStockQuantity(8);
                dto.setCategoryName("Əyləc sistemi");
                dto.setCarBrandName("Toyota");
                dto.setYearRange("2024");
                dto.setEngine("2.0L");
                dto.setOemCode("TY-4455");
                dto.setIsActive(true);

                when(productService.getActiveProductById(1L)).thenReturn(dto);

                mockMvc.perform(get("/products/1"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("product-details"))
                                .andExpect(model().attributeExists("product"))
                                .andExpect(model().attribute("product", dto));
        }

        @Test
        @DisplayName("GET /products/{id} tapılmayan məhsul üçün həqiqi HTTP 404 və error/404 qaytarır")
        void testGetProductDetails_NotFound_Returns404() throws Exception {
                when(productService.getActiveProductById(9999L))
                                .thenThrow(new EntityNotFoundException("Məhsul tapılmadı!"));

                mockMvc.perform(get("/products/9999"))
                                .andExpect(status().isNotFound())
                                .andExpect(view().name("error/404"))
                                .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        @DisplayName("GET /products/{id} deaktiv məhsul üçün həqiqi HTTP 404 və error/404 qaytarır")
        void testGetProductDetails_DeactivatedProduct_Returns404() throws Exception {
                // Mövcud getActiveProductById metodu isActive=false olduqda
                // EntityNotFoundException atır
                when(productService.getActiveProductById(20L))
                                .thenThrow(new EntityNotFoundException("Məhsul tapılmadı!"));

                mockMvc.perform(get("/products/20"))
                                .andExpect(status().isNotFound())
                                .andExpect(view().name("error/404"))
                                .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        @DisplayName("GET /products/{id} qeyri-rəqəm ID olduqda 404 qaytarır")
        void testGetProductDetails_InvalidIdFormat_Returns404() throws Exception {
                mockMvc.perform(get("/products/invalid-id"))
                                .andExpect(status().isNotFound())
                                .andExpect(view().name("error/404"));
        }

        @Test
        @DisplayName("GET /products/{id} stoku 0 olan aktiv məhsulu 200 ilə açır, 'Stokda yoxdur' göstərir və əlavəetməni deaktiv edir")
        void testGetProductDetails_StockZero_DirectLinkAllowed_CartActionsDisabled() throws Exception {
                ProductResponseDto dto = new ProductResponseDto();
                dto.setId(5L);
                dto.setName("Mühərrik kəməri");
                dto.setCode("BLT-001");
                dto.setPrice(new BigDecimal("15.00"));
                dto.setStockQuantity(0);
                dto.setIsActive(true);

                when(productService.getActiveProductById(5L)).thenReturn(dto);

                // 1. Autentifikasiya olunmuş istifadəçi üçün form və düymələr deaktivdir
                mockMvc.perform(get("/products/5").flashAttr("isAuthenticated", true))
                                .andExpect(status().isOk())
                                .andExpect(view().name("product-details"))
                                .andExpect(content().string(containsString("BLT-001")))
                                .andExpect(content().string(containsString("Stokda yoxdur")))
                                .andExpect(content().string(containsString("max=\"0\"")))
                                .andExpect(content().string(containsString("disabled")));

                // 2. Qeydiyyatsız istifadəçi üçün də birbaşa link açılır və "Stokda yoxdur"
                // görünür
                mockMvc.perform(get("/products/5"))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("Stokda yoxdur")));
        }

    @Test
    @DisplayName("GET /products/{id} stoku 7 olan aktiv məhsulda min=1, step=1 və max=7 təyin edir")
    void testGetProductDetails_Stock7_RendersCorrectLimits() throws Exception {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(7L);
        dto.setName("Fara");
        dto.setCode("LGT-777");
        dto.setPrice(new BigDecimal("80.00"));
        dto.setStockQuantity(7);
        dto.setIsActive(true);

        when(productService.getActiveProductById(7L)).thenReturn(dto);

        mockMvc.perform(get("/products/7").flashAttr("isAuthenticated", true))
                .andExpect(status().isOk())
                .andExpect(view().name("product-details"))
                .andExpect(content().string(containsString("LGT-777")))
                .andExpect(content().string(containsString("max=\"7\"")))
                .andExpect(content().string(containsString("min=\"1\"")))
                .andExpect(content().string(containsString("step=\"1\"")));
    }
}
