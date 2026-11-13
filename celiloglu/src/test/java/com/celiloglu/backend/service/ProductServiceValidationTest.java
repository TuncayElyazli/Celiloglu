package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.request.ProductRequestDto;
import com.celiloglu.backend.dto.response.ProductResponseDto;
import com.celiloglu.backend.entity.CarBrand;
import com.celiloglu.backend.entity.Category;
import com.celiloglu.backend.entity.Product;
import com.celiloglu.backend.repository.CarBrandRepository;
import com.celiloglu.backend.repository.CategoryRepository;
import com.celiloglu.backend.repository.ProductRepository;
import com.celiloglu.backend.service.impl.ProductServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceValidationTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CarBrandRepository carBrandRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category testCategory;
    private CarBrand testCarBrand;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Mühərrik hissələri");

        testCarBrand = new CarBrand();
        testCarBrand.setId(2L);
        testCarBrand.setName("Toyota");
    }

    private ProductRequestDto createValidDto() {
        ProductRequestDto dto = new ProductRequestDto();
        dto.setName("Yağ filtri");
        dto.setCode("YAG-101");
        dto.setPrice(new BigDecimal("25.50"));
        dto.setStockQuantity(10);
        dto.setCategoryId(1L);
        dto.setCarBrandId(2L);
        dto.setYearRange("2024");
        dto.setDescription("Orijinal filtr");
        dto.setEngine("2.5L Hybrid");
        dto.setOemCode("OEM-TY-9988");
        dto.setIsActive(true);
        return dto;
    }

    @Test
    @DisplayName("Uğurlu məhsul yaradılması - bütün sahələr düzgün")
    void testCreateProduct_Success() {
        ProductRequestDto dto = createValidDto();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(carBrandRepository.findById(2L)).thenReturn(Optional.of(testCarBrand));
        when(productRepository.findByCode("YAG-101")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });

        ProductResponseDto response = productService.createProduct(dto);

        assertNotNull(response);
        assertEquals("Yağ filtri", response.getName());
        assertEquals("YAG-101", response.getCode());
        assertEquals(new BigDecimal("25.50"), response.getPrice());
        assertEquals(10, response.getStockQuantity());
        assertEquals("2024", response.getYearRange());
        assertEquals(1L, response.getCategoryId());
        assertEquals(2L, response.getCarBrandId());
    }

    @Test
    @DisplayName("name boş və ya yalnız boşluq olduqda rədd edilir")
    void testName_Blank_Rejected() {
        ProductRequestDto dto = createValidDto();
        dto.setName("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("Məhsulun adı boş ola bilməz!", ex.getMessage());

        dto.setName(null);
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));
    }

    @Test
    @DisplayName("name uzunluğu 255 simvolu keçdikdə rədd edilir")
    void testName_TooLong_Rejected() {
        ProductRequestDto dto = createValidDto();
        dto.setName("A".repeat(256));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertTrue(ex.getMessage().contains("255"));
    }

    @Test
    @DisplayName("code boş və ya yalnız boşluq olduqda rədd edilir")
    void testCode_Blank_Rejected() {
        ProductRequestDto dto = createValidDto();
        dto.setCode("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("Məhsulun kodu boş ola bilməz!", ex.getMessage());

        dto.setCode(null);
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));
    }

    @Test
    @DisplayName("Təkrar kodla yeni məhsul yaradılması rədd edilir")
    void testCode_Duplicate_OnCreate_Rejected() {
        ProductRequestDto dto = createValidDto();

        Product existing = new Product();
        existing.setId(50L);
        existing.setCode("YAG-101");
        when(productRepository.findByCode("YAG-101")).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("Bu kodla məhsul artıq mövcuddur!", ex.getMessage());
    }

    @Test
    @DisplayName("Redaktədə eyni məhsulun öz kodunu saxlamasına icazə verilir")
    void testCode_SameProduct_OnUpdate_Allowed() {
        ProductRequestDto dto = createValidDto();

        Product existing = new Product();
        existing.setId(100L);
        existing.setCode("YAG-101");

        when(productRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(productRepository.findByCode("YAG-101")).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(carBrandRepository.findById(2L)).thenReturn(Optional.of(testCarBrand));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> productService.updateProduct(100L, dto));
    }

    @Test
    @DisplayName("Redaktədə başqa məhsulun kodunu mənimsəmək rədd edilir")
    void testCode_AnotherProduct_OnUpdate_Rejected() {
        ProductRequestDto dto = createValidDto();

        Product currentProduct = new Product();
        currentProduct.setId(100L);

        Product otherProduct = new Product();
        otherProduct.setId(200L);
        otherProduct.setCode("YAG-101");

        when(productRepository.findById(100L)).thenReturn(Optional.of(currentProduct));
        when(productRepository.findByCode("YAG-101")).thenReturn(Optional.of(otherProduct));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.updateProduct(100L, dto));
        assertEquals("Bu kodla məhsul artıq mövcuddur!", ex.getMessage());
    }

    @Test
    @DisplayName("price null, 0 və mənfi olduqda rədd edilir")
    void testPrice_NullZeroNegative_Rejected() {
        ProductRequestDto dto = createValidDto();

        dto.setPrice(null);
        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("Qiymət daxil edilməlidir!", exNull.getMessage());

        dto.setPrice(BigDecimal.ZERO);
        IllegalArgumentException exZero = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("Qiymət minimum 0.01 AZN olmalıdır!", exZero.getMessage());

        dto.setPrice(new BigDecimal("-5.00"));
        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("Qiymət minimum 0.01 AZN olmalıdır!", exNeg.getMessage());
    }

    @Test
    @DisplayName("price 2-dən çox onluq rəqəmlə olduqda rədd edilir (yuvarlaqlaşdırılmır)")
    void testPrice_MoreThanTwoDecimals_Rejected() {
        ProductRequestDto dto = createValidDto();
        dto.setPrice(new BigDecimal("10.555"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("Qiymət maksimum 2 onluq rəqəmdən ibarət ola bilər!", ex.getMessage());
    }

    @Test
    @DisplayName("price 0.01 AZN və 2 onluq qəbul edilir")
    void testPrice_ValidMin() {
        ProductRequestDto dto = createValidDto();
        dto.setPrice(new BigDecimal("0.01"));

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(carBrandRepository.findById(2L)).thenReturn(Optional.of(testCarBrand));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> productService.createProduct(dto));
    }

    @Test
    @DisplayName("stockQuantity null və mənfi olduqda rədd edilir; 0 qəbul edilir (stok >= 0)")
    void testStockQuantity_Validation() {
        ProductRequestDto dto = createValidDto();

        // null rədd edilir
        dto.setStockQuantity(null);
        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("Stok miqdarı daxil edilməlidir!", exNull.getMessage());

        // mənfi rədd edilir
        dto.setStockQuantity(-1);
        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("Stok miqdarı mənfi ola bilməz!", exNeg.getMessage());

        dto.setStockQuantity(-10);
        IllegalArgumentException exNeg10 = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("Stok miqdarı mənfi ola bilməz!", exNeg10.getMessage());

        // 0 qəbul edilir (stok >= 0)
        dto.setStockQuantity(0);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(carBrandRepository.findById(2L)).thenReturn(Optional.of(testCarBrand));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> productService.createProduct(dto));
    }

    @Test
    @DisplayName("Redaktə zamanı qiymət: null, 0, -10 rədd edilir; stok: mənfi rədd edilir, 0 qəbul edilir")
    void testUpdateProduct_PriceAndStock_Validation() {
        Product existing = new Product();
        existing.setId(100L);
        existing.setCode("YAG-101");
        existing.setStockQuantity(10);
        existing.setPrice(new BigDecimal("25.50"));

        when(productRepository.findById(100L)).thenReturn(Optional.of(existing));

        // Qiymət null rədd edilir
        ProductRequestDto dtoNullPrice = createValidDto();
        dtoNullPrice.setPrice(null);
        IllegalArgumentException exNullPrice = assertThrows(IllegalArgumentException.class,
                () -> productService.updateProduct(100L, dtoNullPrice));
        assertEquals("Qiymət daxil edilməlidir!", exNullPrice.getMessage());

        // Qiymət 0 rədd edilir
        ProductRequestDto dtoZeroPrice = createValidDto();
        dtoZeroPrice.setPrice(BigDecimal.ZERO);
        IllegalArgumentException exZeroPrice = assertThrows(IllegalArgumentException.class,
                () -> productService.updateProduct(100L, dtoZeroPrice));
        assertEquals("Qiymət minimum 0.01 AZN olmalıdır!", exZeroPrice.getMessage());

        // Qiymət -10 rədd edilir
        ProductRequestDto dtoNegPrice = createValidDto();
        dtoNegPrice.setPrice(new BigDecimal("-10.00"));
        IllegalArgumentException exNegPrice = assertThrows(IllegalArgumentException.class,
                () -> productService.updateProduct(100L, dtoNegPrice));
        assertEquals("Qiymət minimum 0.01 AZN olmalıdır!", exNegPrice.getMessage());

        // Stok null rədd edilir
        ProductRequestDto dtoNullStock = createValidDto();
        dtoNullStock.setStockQuantity(null);
        IllegalArgumentException exNullStock = assertThrows(IllegalArgumentException.class,
                () -> productService.updateProduct(100L, dtoNullStock));
        assertEquals("Stok miqdarı daxil edilməlidir!", exNullStock.getMessage());

        // Stok mənfi (-10) rədd edilir
        ProductRequestDto dtoNegStock = createValidDto();
        dtoNegStock.setStockQuantity(-10);
        IllegalArgumentException exNegStock = assertThrows(IllegalArgumentException.class,
                () -> productService.updateProduct(100L, dtoNegStock));
        assertEquals("Stok miqdarı mənfi ola bilməz!", exNegStock.getMessage());

        // Stok 0 qəbul edilir
        ProductRequestDto dtoZeroStock = createValidDto();
        dtoZeroStock.setStockQuantity(0);
        when(productRepository.findByCode("YAG-101")).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(carBrandRepository.findById(2L)).thenReturn(Optional.of(testCarBrand));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> productService.updateProduct(100L, dtoZeroStock));
    }

    @Test
    @DisplayName("yearRange (yaratma): cari il qəbul edilir, 2030 və cari ildən böyük illər rədd edilir")
    void testCreateProduct_YearRange_Validation() {
        int currentYear = Year.now().getValue();
        ProductRequestDto dto = createValidDto();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(carBrandRepository.findById(2L)).thenReturn(Optional.of(testCarBrand));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Boş və ya null qala bilər (istəyə bağlıdır)
        dto.setYearRange(null);
        assertDoesNotThrow(() -> productService.createProduct(dto));

        dto.setYearRange("   ");
        assertDoesNotThrow(() -> productService.createProduct(dto));

        // Cari il qəbul edilir
        dto.setYearRange(String.valueOf(currentYear));
        assertDoesNotThrow(() -> productService.createProduct(dto));

        // 2030 rədd edilir
        dto.setYearRange("2030");
        IllegalArgumentException ex2030 = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("İl cari ildən böyük ola bilməz.", ex2030.getMessage());

        // Cari ildən böyük istənilən il rədd edilir
        dto.setYearRange(String.valueOf(currentYear + 1));
        IllegalArgumentException exNext = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("İl cari ildən böyük ola bilməz.", exNext.getMessage());

        // Range: 2015-2020 qəbul edilir
        dto.setYearRange("2015-2020");
        assertDoesNotThrow(() -> productService.createProduct(dto));

        // Range: son il cari ildən böyük olduqda (2020-2030) rədd edilir
        dto.setYearRange("2020-2030");
        IllegalArgumentException exRangeFuture = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("İl cari ildən böyük ola bilməz.", exRangeFuture.getMessage());

        // Range: başlanğıc ili son ildən böyük olduqda rədd edilir
        dto.setYearRange("2025-2020");
        IllegalArgumentException exInverted = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("Başlanğıc ili son ildən böyük ola bilməz!", exInverted.getMessage());

        // Yanlış formatlar rədd edilir
        dto.setYearRange("202");
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));

        dto.setYearRange("abcd");
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));
    }

    @Test
    @DisplayName("yearRange (redaktə): cari il qəbul edilir, 2030 rədd edilir")
    void testUpdateProduct_YearRange_Validation() {
        int currentYear = Year.now().getValue();
        Product existing = new Product();
        existing.setId(100L);
        existing.setCode("YAG-101");

        when(productRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(productRepository.findByCode("YAG-101")).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(carBrandRepository.findById(2L)).thenReturn(Optional.of(testCarBrand));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Cari il qəbul edilir
        ProductRequestDto dtoCurrent = createValidDto();
        dtoCurrent.setYearRange(String.valueOf(currentYear));
        assertDoesNotThrow(() -> productService.updateProduct(100L, dtoCurrent));

        // 2030 rədd edilir
        ProductRequestDto dto2030 = createValidDto();
        dto2030.setYearRange("2030");
        IllegalArgumentException ex2030 = assertThrows(IllegalArgumentException.class,
                () -> productService.updateProduct(100L, dto2030));
        assertEquals("İl cari ildən böyük ola bilməz.", ex2030.getMessage());

        // Range 2015-2020 qəbul edilir
        ProductRequestDto dtoRange = createValidDto();
        dtoRange.setYearRange("2015-2020");
        assertDoesNotThrow(() -> productService.updateProduct(100L, dtoRange));

        // Range 2020-2030 rədd edilir
        ProductRequestDto dtoRange2030 = createValidDto();
        dtoRange2030.setYearRange("2020-2030");
        IllegalArgumentException exRange2030 = assertThrows(IllegalArgumentException.class,
                () -> productService.updateProduct(100L, dtoRange2030));
        assertEquals("İl cari ildən böyük ola bilməz.", exRange2030.getMessage());
    }

    @Test
    @DisplayName("categoryId məcburidir, müsbət olmalıdır və bazada tapılmalıdır")
    void testCategory_Validation() {
        ProductRequestDto dto = createValidDto();

        // null rədd edilir
        dto.setCategoryId(null);
        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(dto));
        assertEquals("Kateqoriya seçilməlidir!", exNull.getMessage());

        // 0 və mənfi rədd edilir
        dto.setCategoryId(0L);
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));
        dto.setCategoryId(-1L);
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));

        // Bazada tapılmadıqda EntityNotFoundException atılır
        dto.setCategoryId(999L);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> productService.createProduct(dto));
    }

    @Test
    @DisplayName("carBrandId istəyə bağlıdır, amma verilirsə müsbət və mövcud olmalıdır")
    void testCarBrand_Validation() {
        ProductRequestDto dto = createValidDto();

        // null carBrandId icazəlidir
        dto.setCarBrandId(null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertDoesNotThrow(() -> productService.createProduct(dto));

        // <= 0 rədd edilir
        dto.setCarBrandId(0L);
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));

        // Bazada tapılmadıqda EntityNotFoundException
        dto.setCarBrandId(888L);
        when(carBrandRepository.findById(888L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> productService.createProduct(dto));
    }

    @Test
    @DisplayName("description, engine, oemCode maksimum 255 simvol olmalıdır, OEM yalnız rəqəmlə məhdudlaşmır")
    void testTextLengths_And_OemFormat() {
        ProductRequestDto dto = createValidDto();
        dto.setOemCode("ABC-123/XYZ-99#"); // Alfanumerik və simvollar qəbul edilir

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(carBrandRepository.findById(2L)).thenReturn(Optional.of(testCarBrand));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertDoesNotThrow(() -> productService.createProduct(dto));

        // description > 255
        dto.setDescription("X".repeat(256));
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));
        dto.setDescription("Qısa təsvir");

        // engine > 255
        dto.setEngine("E".repeat(256));
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));
        dto.setEngine("2.0 Turbo");

        // oemCode > 255
        dto.setOemCode("O".repeat(256));
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));
    }
}
