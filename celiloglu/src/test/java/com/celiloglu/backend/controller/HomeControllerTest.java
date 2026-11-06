package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.request.ProductFilterRequestDto;
import com.celiloglu.backend.dto.response.ProductResponseDto;
import com.celiloglu.backend.service.ProductFormOptionsService;
import com.celiloglu.backend.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @Mock
    private ProductFormOptionsService optionsService;

    @InjectMocks
    private HomeController homeController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(homeController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    @DisplayName("GET / ana səhifəni 200 statusu və index şablonu ilə açır")
    void testIndex_Success() throws Exception {
        when(optionsService.getCategories()).thenReturn(Collections.emptyList());
        when(optionsService.getCarBrands()).thenReturn(Collections.emptyList());
        when(optionsService.getYears()).thenReturn(Collections.emptyList());

        ProductResponseDto productDto = new ProductResponseDto();
        productDto.setId(10L);
        productDto.setName("Tormoz bəndi");
        productDto.setCode("asf-321");
        productDto.setPrice(new BigDecimal("40.00"));
        productDto.setStockQuantity(15);

        Page<ProductResponseDto> page = new PageImpl<>(List.of(productDto));
        when(productService.searchActiveProducts(any(ProductFilterRequestDto.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("productPage"))
                .andExpect(model().attribute("productCount", 1L));
    }

    @Test
    @DisplayName("GET /?code=... axtarış parametri ilə HomeController filtri işləyir")
    void testIndex_WithCodeFilter() throws Exception {
        when(optionsService.getCategories()).thenReturn(Collections.emptyList());
        when(optionsService.getCarBrands()).thenReturn(Collections.emptyList());
        when(optionsService.getYears()).thenReturn(Collections.emptyList());

        ProductResponseDto productDto = new ProductResponseDto();
        productDto.setId(2L);
        productDto.setName("Tormoz");
        productDto.setCode("asf-321");
        productDto.setPrice(new BigDecimal("400.00"));

        Page<ProductResponseDto> page = new PageImpl<>(List.of(productDto));
        when(productService.searchActiveProducts(any(ProductFilterRequestDto.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/").param("code", "asf-321"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attribute("productCount", 1L));
    }

    @Test
    @DisplayName("GET / index.html şablonunda məhsul kartının miqdar sahəsində min=1, step=1 və max=7 render olunur")
    void testIndex_QuantityLimits_RenderedCorrectly() throws Exception {
        when(optionsService.getCategories()).thenReturn(Collections.emptyList());
        when(optionsService.getCarBrands()).thenReturn(Collections.emptyList());
        when(optionsService.getYears()).thenReturn(Collections.emptyList());

        ProductResponseDto productDto = new ProductResponseDto();
        productDto.setId(7L);
        productDto.setName("Yağ nasosu");
        productDto.setCode("OIL-777");
        productDto.setPrice(new BigDecimal("120.00"));
        productDto.setStockQuantity(7);
        productDto.setIsActive(true);

        Page<ProductResponseDto> page = new PageImpl<>(List.of(productDto));
        when(productService.searchActiveProducts(any(ProductFilterRequestDto.class), any(Pageable.class)))
                .thenReturn(page);

        // SecurityModelAdvice isAuthenticated=true simulyasiyası ilə
        mockMvc.perform(get("/").flashAttr("isAuthenticated", true))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("OIL-777")))
                .andExpect(content().string(containsString("max=\"7\"")))
                .andExpect(content().string(containsString("min=\"1\"")))
                .andExpect(content().string(containsString("step=\"1\"")));
    }
}
