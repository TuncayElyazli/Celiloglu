package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.request.CartItemRequestDto;
import com.celiloglu.backend.dto.response.CartItemResponseDto;
import com.celiloglu.backend.dto.response.CartResponseDto;
import com.celiloglu.backend.entity.Cart;
import com.celiloglu.backend.entity.CartItem;
import com.celiloglu.backend.entity.Product;
import com.celiloglu.backend.entity.User;
import com.celiloglu.backend.repository.CartRepository;
import com.celiloglu.backend.repository.ProductRepository;
import com.celiloglu.backend.repository.UserRepository;
import com.celiloglu.backend.service.impl.CartServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceValidationTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private CartServiceImpl cartService;

    private User activeUser;
    private User inactiveUser;
    private User otherUser;
    private Product activeProduct;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setUsername("testuser");
        activeUser.setEmail("test@example.com");
        activeUser.setIsActive(true);

        inactiveUser = new User();
        inactiveUser.setId(2L);
        inactiveUser.setUsername("inactiveuser");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setIsActive(false);

        otherUser = new User();
        otherUser.setId(99L);
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setIsActive(true);

        activeProduct = new Product();
        activeProduct.setId(101L);
        activeProduct.setName("Əyləc bəndi");
        activeProduct.setCode("BR-101");
        activeProduct.setPrice(new BigDecimal("15.50"));
        activeProduct.setStockQuantity(10);
        activeProduct.setIsActive(true);
    }

    // ==========================================
    // Mövcud getCart testləri (toxunulmaz saxlanılıb)
    // ==========================================

    @Test
    @DisplayName("İstifadəçinin səbəti olmadıqda boş nəticə qaytarmalıdır")
    void getCart_whenCartDoesNotExist_shouldReturnEmptyCart() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        CartResponseDto result = cartService.getCart(1L);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0L, result.getTotalItems());
        assertEquals(BigDecimal.ZERO, result.getTotalPrice());

        verify(cartRepository, never()).save(any());
        verify(cartRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Səbətin daxilində element olmadıqda boş nəticə qaytarmalıdır")
    void getCart_whenCartItemsListIsEmpty_shouldReturnEmptyCart() {
        Cart emptyCart = new Cart();
        emptyCart.setId(10L);
        emptyCart.setUser(activeUser);
        emptyCart.setItems(new ArrayList<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(emptyCart));

        CartResponseDto result = cartService.getCart(1L);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0L, result.getTotalItems());
        assertEquals(BigDecimal.ZERO, result.getTotalPrice());
    }

    @Test
    @DisplayName("Müxtəlif məhsulların miqdar və məbləğ cəmlərini düzgün hesablamalıdır")
    void getCart_withMultipleProducts_shouldCalculateCorrectTotals() {
        Product p1 = new Product();
        p1.setId(101L);
        p1.setName("Əyləc bəndi");
        p1.setCode("BR-101");
        p1.setPrice(new BigDecimal("15.50"));
        p1.setStockQuantity(10);
        p1.setIsActive(true);

        Product p2 = new Product();
        p2.setId(102L);
        p2.setName("Yağ filtri");
        p2.setCode("OF-202");
        p2.setPrice(new BigDecimal("42.00"));
        p2.setStockQuantity(5);
        p2.setIsActive(true);

        CartItem item1 = new CartItem();
        item1.setId(1L);
        item1.setProduct(p1);
        item1.setQuantity(2); // 15.50 * 2 = 31.00

        CartItem item2 = new CartItem();
        item2.setId(2L);
        item2.setProduct(p2);
        item2.setQuantity(3); // 42.00 * 3 = 126.00

        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(activeUser);
        cart.setItems(List.of(item1, item2));

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartResponseDto result = cartService.getCart(1L);

        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        assertEquals(5L, result.getTotalItems());
        assertEquals(new BigDecimal("157.00"), result.getTotalPrice());

        CartItemResponseDto dto1 = result.getItems().get(0);
        assertEquals(101L, dto1.getProductId());
        assertEquals(2, dto1.getQuantity());
        assertEquals(new BigDecimal("15.50"), dto1.getUnitPrice());
        assertEquals(new BigDecimal("31.00"), dto1.getTotalPrice());
        assertTrue(dto1.getAvailable());
        assertNull(dto1.getAvailabilityMessage());

        CartItemResponseDto dto2 = result.getItems().get(1);
        assertEquals(102L, dto2.getProductId());
        assertEquals(3, dto2.getQuantity());
        assertEquals(new BigDecimal("42.00"), dto2.getUnitPrice());
        assertEquals(new BigDecimal("126.00"), dto2.getTotalPrice());
        assertTrue(dto2.getAvailable());
        assertNull(dto2.getAvailabilityMessage());
    }

    @Test
    @DisplayName("Yanlış və ya qeyri-müsbət userId xəta atmalıdır")
    void getCart_withInvalidUserId_shouldThrowIllegalArgumentException() {
        IllegalArgumentException exNull = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.getCart(null)
        );
        assertEquals("İstifadəçi ID-si müsbət ədəd olmalıdır!", exNull.getMessage());

        IllegalArgumentException exZero = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.getCart(0L)
        );
        assertEquals("İstifadəçi ID-si müsbət ədəd olmalıdır!", exZero.getMessage());

        IllegalArgumentException exNeg = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.getCart(-10L)
        );
        assertEquals("İstifadəçi ID-si müsbət ədəd olmalıdır!", exNeg.getMessage());

        verifyNoInteractions(userRepository);
        verifyNoInteractions(cartRepository);
    }

    @Test
    @DisplayName("Tapılmayan istifadəçi EntityNotFoundException atmalıdır")
    void getCart_whenUserNotFound_shouldThrowEntityNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> cartService.getCart(999L)
        );

        assertTrue(ex.getMessage().contains("İstifadəçi tapılmadı! ID: 999"));
        verify(cartRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("Deaktiv istifadəçi IllegalStateException atmalıdır")
    void getCart_whenUserIsInactive_shouldThrowIllegalStateException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(inactiveUser));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> cartService.getCart(2L)
        );

        assertEquals("İstifadəçi hesabı deaktivdir!", ex.getMessage());
        verify(cartRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("Deaktiv edilmiş məhsul səbətdən silinməməli, xəbərdarlıqla göstərilməlidir")
    void getCart_whenProductIsDeactivated_shouldKeepItemWithWarning() {
        Product inactiveProduct = new Product();
        inactiveProduct.setId(201L);
        inactiveProduct.setName("Köhnə model akumulyator");
        inactiveProduct.setCode("BAT-OLD");
        inactiveProduct.setPrice(new BigDecimal("120.00"));
        inactiveProduct.setStockQuantity(10);
        inactiveProduct.setIsActive(false);

        CartItem item = new CartItem();
        item.setId(5L);
        item.setProduct(inactiveProduct);
        item.setQuantity(1);

        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(activeUser);
        cart.setItems(List.of(item));

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartResponseDto result = cartService.getCart(1L);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(1L, result.getTotalItems());
        assertEquals(new BigDecimal("120.00"), result.getTotalPrice());

        CartItemResponseDto itemDto = result.getItems().get(0);
        assertFalse(itemDto.getAvailable());
        assertNotNull(itemDto.getAvailabilityMessage());
        assertTrue(itemDto.getAvailabilityMessage().contains("deaktiv"));
    }

    @Test
    @DisplayName("Stoku çatmayan və ya bitmiş məhsul xəbərdarlıqla göstərilməlidir")
    void getCart_whenStockIsInsufficient_shouldSetAvailableFalseWithMessage() {
        Product lowStockProduct = new Product();
        lowStockProduct.setId(301L);
        lowStockProduct.setName("Şam");
        lowStockProduct.setCode("SP-1");
        lowStockProduct.setPrice(new BigDecimal("10.00"));
        lowStockProduct.setStockQuantity(2); // stok: 2, tələb olunan: 5
        lowStockProduct.setIsActive(true);

        Product outOfStockProduct = new Product();
        outOfStockProduct.setId(302L);
        outOfStockProduct.setName("Kəmər");
        outOfStockProduct.setCode("BL-2");
        outOfStockProduct.setPrice(new BigDecimal("25.00"));
        outOfStockProduct.setStockQuantity(0); // stok: 0, tələb olunan: 1
        outOfStockProduct.setIsActive(true);

        CartItem item1 = new CartItem();
        item1.setId(11L);
        item1.setProduct(lowStockProduct);
        item1.setQuantity(5);

        CartItem item2 = new CartItem();
        item2.setId(12L);
        item2.setProduct(outOfStockProduct);
        item2.setQuantity(1);

        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(activeUser);
        cart.setItems(List.of(item1, item2));

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartResponseDto result = cartService.getCart(1L);

        assertNotNull(result);
        assertEquals(2, result.getItems().size());

        CartItemResponseDto dto1 = result.getItems().get(0);
        assertFalse(dto1.getAvailable());
        assertNotNull(dto1.getAvailabilityMessage());
        assertTrue(dto1.getAvailabilityMessage().contains("Stokda cəmi 2 ədəd mövcuddur"));

        CartItemResponseDto dto2 = result.getItems().get(1);
        assertFalse(dto2.getAvailable());
        assertNotNull(dto2.getAvailabilityMessage());
        assertTrue(dto2.getAvailabilityMessage().contains("stokda bitib"));
    }

    @Test
    @DisplayName("Oxuma zamanı save və ya delete çağırılmamalıdır")
    void getCart_shouldNeverCallSaveOrDeleteOnRepositories() {
        Product p = new Product();
        p.setId(401L);
        p.setName("Filtr");
        p.setCode("FL-1");
        p.setPrice(new BigDecimal("20.00"));
        p.setStockQuantity(10);
        p.setIsActive(true);

        CartItem item = new CartItem();
        item.setId(21L);
        item.setProduct(p);
        item.setQuantity(2);

        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(activeUser);
        cart.setItems(List.of(item));

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        cartService.getCart(1L);

        verify(cartRepository, never()).save(any());
        verify(cartRepository, never()).delete(any());
        verify(cartRepository, never()).deleteAll(any());
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).delete(any());
    }

    // ==========================================
    // Yeni yazma əməliyyatlarının unit testləri
    // ==========================================

    @Test
    @DisplayName("Səbət olmadıqda ilk əlavəetmə səbət yaradıb əlaqələri düzgün qurmalıdır")
    void addItemToCart_whenCartDoesNotExist_shouldCreateCartAndEstablishRelations() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemRequestDto request = new CartItemRequestDto(101L, 3);
        CartResponseDto result = cartService.addItemToCart(1L, request);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(3L, result.getTotalItems());
        assertEquals(new BigDecimal("46.50"), result.getTotalPrice());

        // Hər iki tərəfli əlaqələrin qurulmasını yoxla
        assertNotNull(activeUser.getCart());
        assertEquals(activeUser, activeUser.getCart().getUser());
        assertEquals(1, activeUser.getCart().getItems().size());
        CartItem createdItem = activeUser.getCart().getItems().get(0);
        assertEquals(activeUser.getCart(), createdItem.getCart());
        assertEquals(activeProduct, createdItem.getProduct());
        assertEquals(3, createdItem.getQuantity());

        // Bazadakı məhsul stokunun toxunulmadığını yoxla
        assertEquals(10, activeProduct.getStockQuantity());
        verify(productRepository, never()).save(any());

        // Kilidli axtarışın çağırıldığını yoxla
        verify(userRepository).findByIdWithLock(1L);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Eyni məhsul təkrar əlavə edildikdə yeni sətir yaratmamalı, mövcud miqdarı artırmalıdır")
    void addItemToCart_whenProductAlreadyExistsInCart_shouldIncreaseQuantity() {
        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(activeUser);
        activeUser.setCart(cart);

        CartItem existingItem = new CartItem();
        existingItem.setId(50L);
        existingItem.setCart(cart);
        existingItem.setProduct(activeProduct);
        existingItem.setQuantity(2);
        cart.getItems().add(existingItem);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemRequestDto request = new CartItemRequestDto(101L, 4);
        CartResponseDto result = cartService.addItemToCart(1L, request);

        assertNotNull(result);
        assertEquals(1, result.getItems().size(), "Səbətdə yeni sətir yaranmamalıdır");
        assertEquals(6L, result.getTotalItems(), "Cəmi say 2 + 4 = 6 olmalıdır");
        assertEquals(new BigDecimal("93.00"), result.getTotalPrice(), "15.50 * 6 = 93.00");
        assertEquals(6, existingItem.getQuantity());

        // Stok azaldılmamalıdır
        assertEquals(10, activeProduct.getStockQuantity());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mövcud məhsulun miqdarını dəyişdirmə (updateItemQuantity) sayı yeniləməlidir")
    void updateItemQuantity_whenValid_shouldUpdateQuantity() {
        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(activeUser);

        CartItem existingItem = new CartItem();
        existingItem.setId(50L);
        existingItem.setCart(cart);
        existingItem.setProduct(activeProduct);
        existingItem.setQuantity(2);
        cart.getItems().add(existingItem);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponseDto result = cartService.updateItemQuantity(1L, 101L, 7);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(7L, result.getTotalItems());
        assertEquals(new BigDecimal("108.50"), result.getTotalPrice(), "15.50 * 7 = 108.50");
        assertEquals(7, existingItem.getQuantity());

        // Stok dəyişilməməlidir
        assertEquals(10, activeProduct.getStockQuantity());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Əlavəetmədə nəticə miqdarı stoku aşdıqda xəta atmalıdır")
    void addItemToCart_whenExceedsStock_shouldThrowIllegalArgumentException() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // activeProduct stoku 10-dur, 11 əlavə etmək istəyirik
        CartItemRequestDto request = new CartItemRequestDto(101L, 11);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addItemToCart(1L, request)
        );

        assertTrue(ex.getMessage().contains("mövcud stokdan (10) çox ola bilməz"));
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Təkrar əlavə zamanı cəmi miqdar stoku aşdıqda xəta atmalıdır")
    void addItemToCart_whenExistingPlusNewExceedsStock_shouldThrowIllegalArgumentException() {
        Cart cart = new Cart();
        cart.setUser(activeUser);
        CartItem existingItem = new CartItem();
        existingItem.setProduct(activeProduct);
        existingItem.setQuantity(8);
        cart.getItems().add(existingItem);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        // Stok 10, səbətdə 8 var, 3 əlavə etmək istəyirik (8+3=11 > 10)
        CartItemRequestDto request = new CartItemRequestDto(101L, 3);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addItemToCart(1L, request)
        );

        assertTrue(ex.getMessage().contains("mövcud stokdan (10) çox ola bilməz"));
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Miqdarların toplanmasında Integer overflow nəzərə alınmalı və xəta atmalıdır")
    void addItemToCart_whenQuantityOverflows_shouldThrowIllegalArgumentException() {
        Cart cart = new Cart();
        cart.setUser(activeUser);
        CartItem existingItem = new CartItem();
        existingItem.setProduct(activeProduct);
        existingItem.setQuantity(Integer.MAX_VALUE - 2);
        cart.getItems().add(existingItem);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartItemRequestDto request = new CartItemRequestDto(101L, 10);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addItemToCart(1L, request)
        );

        assertTrue(ex.getMessage().contains("maksimum həddi aşır") || ex.getMessage().contains("stokdan"));
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Yeniləmədə (updateItemQuantity) miqdar stoku aşdıqda xəta atmalıdır")
    void updateItemQuantity_whenExceedsStock_shouldThrowIllegalArgumentException() {
        Cart cart = new Cart();
        cart.setUser(activeUser);
        CartItem existingItem = new CartItem();
        existingItem.setProduct(activeProduct);
        existingItem.setQuantity(2);
        cart.getItems().add(existingItem);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateItemQuantity(1L, 101L, 15)
        );

        assertTrue(ex.getMessage().contains("mövcud stokdan (10) çox ola bilməz"));
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Yanlış miqdar, mənfi və ya sıfır miqdar rədd edilməlidir")
    void writeMethods_withInvalidQuantity_shouldThrowIllegalArgumentException() {
        // addItemToCart
        assertThrows(IllegalArgumentException.class,
                () -> cartService.addItemToCart(1L, new CartItemRequestDto(101L, null)));
        assertThrows(IllegalArgumentException.class,
                () -> cartService.addItemToCart(1L, new CartItemRequestDto(101L, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> cartService.addItemToCart(1L, new CartItemRequestDto(101L, -5)));

        // addItemByCode
        assertThrows(IllegalArgumentException.class,
                () -> cartService.addItemByCode(1L, "BR-101", null));
        assertThrows(IllegalArgumentException.class,
                () -> cartService.addItemByCode(1L, "BR-101", 0));
        assertThrows(IllegalArgumentException.class,
                () -> cartService.addItemByCode(1L, "BR-101", -2));

        // updateItemQuantity
        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateItemQuantity(1L, 101L, null));
        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateItemQuantity(1L, 101L, 0));
        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateItemQuantity(1L, 101L, -3));
    }

    @Test
    @DisplayName("Deaktiv məhsul səbətə əlavə edilə bilməz")
    void addItemToCart_whenProductIsInactive_shouldThrowIllegalStateException() {
        activeProduct.setIsActive(false);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));

        CartItemRequestDto request = new CartItemRequestDto(101L, 1);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> cartService.addItemToCart(1L, request)
        );

        assertTrue(ex.getMessage().contains("deaktiv"));
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Stoku bitmiş, sıfır və ya mənfi olan məhsul səbətə əlavə edilə bilməz")
    void addItemToCart_whenStockZeroOrNegative_shouldThrowIllegalStateException() {
        activeProduct.setStockQuantity(0);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));

        CartItemRequestDto request = new CartItemRequestDto(101L, 1);

        IllegalStateException exZero = assertThrows(
                IllegalStateException.class,
                () -> cartService.addItemToCart(1L, request)
        );
        assertTrue(exZero.getMessage().contains("stokda yoxdur"));

        activeProduct.setStockQuantity(-5);
        IllegalStateException exNeg = assertThrows(
                IllegalStateException.class,
                () -> cartService.addItemToCart(1L, request)
        );
        assertTrue(exNeg.getMessage().contains("stokda yoxdur"));

        activeProduct.setStockQuantity(null);
        IllegalStateException exNull = assertThrows(
                IllegalStateException.class,
                () -> cartService.addItemToCart(1L, request)
        );
        assertTrue(exNull.getMessage().contains("stokda yoxdur"));
    }

    @Test
    @DisplayName("Qiyməti null və ya qeyri-müsbət olan məhsul qəbul edilməməlidir")
    void addItemToCart_whenPriceIsInvalid_shouldThrowIllegalStateException() {
        activeProduct.setPrice(BigDecimal.ZERO);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));

        CartItemRequestDto request = new CartItemRequestDto(101L, 1);

        IllegalStateException exZero = assertThrows(
                IllegalStateException.class,
                () -> cartService.addItemToCart(1L, request)
        );
        assertTrue(exZero.getMessage().contains("qiyməti düzgün deyil"));

        activeProduct.setPrice(new BigDecimal("-10.00"));
        IllegalStateException exNeg = assertThrows(
                IllegalStateException.class,
                () -> cartService.addItemToCart(1L, request)
        );
        assertTrue(exNeg.getMessage().contains("qiyməti düzgün deyil"));

        activeProduct.setPrice(null);
        IllegalStateException exNull = assertThrows(
                IllegalStateException.class,
                () -> cartService.addItemToCart(1L, request)
        );
        assertTrue(exNull.getMessage().contains("qiyməti düzgün deyil"));
    }

    @Test
    @DisplayName("Kodla əlavəetmə: kodu trim etməli, boş kodu rədd etməli və findByCodeIgnoreCase istifadə etməlidir")
    void addItemByCode_shouldTrimCodeAndUseFindByCodeIgnoreCase() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findByCodeIgnoreCase("BR-101")).thenReturn(List.of(activeProduct));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Boşluqlu kod göndərilir
        CartResponseDto result = cartService.addItemByCode(1L, "  BR-101  ", 2);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(2L, result.getTotalItems());
        assertEquals(new BigDecimal("31.00"), result.getTotalPrice());

        verify(productRepository).findByCodeIgnoreCase("BR-101");
    }

    @Test
    @DisplayName("Kodla əlavəetmə: böyük, kiçik və qarışıq hərflərlə eyni məhsulu tapmalıdır (məs: ASF-321, asf-321, AsF-321)")
    void addItemByCode_shouldFindProductCaseInsensitively() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findByCodeIgnoreCase("ASF-321")).thenReturn(List.of(activeProduct));
        when(productRepository.findByCodeIgnoreCase("asf-321")).thenReturn(List.of(activeProduct));
        when(productRepository.findByCodeIgnoreCase("AsF-321")).thenReturn(List.of(activeProduct));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponseDto res1 = cartService.addItemByCode(1L, "ASF-321", 1);
        assertNotNull(res1);

        CartResponseDto res2 = cartService.addItemByCode(1L, "asf-321", 1);
        assertNotNull(res2);

        CartResponseDto res3 = cartService.addItemByCode(1L, "AsF-321", 1);
        assertNotNull(res3);

        verify(productRepository).findByCodeIgnoreCase("ASF-321");
        verify(productRepository).findByCodeIgnoreCase("asf-321");
        verify(productRepository).findByCodeIgnoreCase("AsF-321");
    }

    @Test
    @DisplayName("Kodla əlavəetmə: məhsul tapılmadıqda EntityNotFoundException atmalıdır")
    void addItemByCode_whenNotFound_shouldThrowEntityNotFoundException() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findByCodeIgnoreCase("NOT-FOUND")).thenReturn(List.of());

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> cartService.addItemByCode(1L, "NOT-FOUND", 1)
        );
        assertTrue(ex.getMessage().contains("Məhsul tapılmadı! Kod: NOT-FOUND"));
    }

    @Test
    @DisplayName("Kodla əlavəetmə: hərf fərqinə görə bir neçə məhsul tapılarsa özbaşına birini seçməməli və xəta atmalıdır")
    void addItemByCode_whenMultipleProductsMatchCaseInsensitively_shouldThrowIllegalStateException() {
        Product duplicateProduct = new Product();
        duplicateProduct.setId(102L);
        duplicateProduct.setCode("asf-321");

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findByCodeIgnoreCase("asf-321")).thenReturn(List.of(activeProduct, duplicateProduct));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> cartService.addItemByCode(1L, "asf-321", 1)
        );
        assertTrue(ex.getMessage().contains("bir neçə məhsul tapıldı"));
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Kodla əlavəetmədə boş və ya null kod rədd edilməlidir")
    void addItemByCode_withBlankCode_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cartService.addItemByCode(1L, null, 1));
        assertThrows(IllegalArgumentException.class, () -> cartService.addItemByCode(1L, "", 1));
        assertThrows(IllegalArgumentException.class, () -> cartService.addItemByCode(1L, "    ", 1));

        verifyNoInteractions(productRepository);
        verifyNoInteractions(cartRepository);
    }

    @Test
    @DisplayName("Səbətdən məhsulun silinməsi yalnız həmin səbətdəki məhsulu çıxarmalıdır")
    void removeItemFromCart_shouldRemoveOnlyTargetProduct() {
        Product p2 = new Product();
        p2.setId(102L);
        p2.setName("Yağ filtri");
        p2.setCode("OF-202");
        p2.setPrice(new BigDecimal("20.00"));
        p2.setStockQuantity(5);
        p2.setIsActive(true);

        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(activeUser);

        CartItem item1 = new CartItem();
        item1.setId(1L);
        item1.setCart(cart);
        item1.setProduct(activeProduct);
        item1.setQuantity(2);

        CartItem item2 = new CartItem();
        item2.setId(2L);
        item2.setCart(cart);
        item2.setProduct(p2);
        item2.setQuantity(1);

        cart.getItems().add(item1);
        cart.getItems().add(item2);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponseDto result = cartService.removeItemFromCart(1L, 101L);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(102L, result.getItems().get(0).getProductId());
        assertEquals(1L, result.getTotalItems());
        assertEquals(new BigDecimal("20.00"), result.getTotalPrice());

        assertNull(item1.getCart(), "Silinən elementin cart referansı təmizlənməlidir");
        assertFalse(cart.getItems().contains(item1));
        assertTrue(cart.getItems().contains(item2));

        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("Deaktiv və ya stoku bitmiş məhsul səbətdən silinə bilməlidir")
    void removeItemFromCart_whenProductIsInactiveOrOutOfStock_shouldSucceed() {
        Product inactiveProduct = new Product();
        inactiveProduct.setId(201L);
        inactiveProduct.setName("Deaktiv detal");
        inactiveProduct.setPrice(new BigDecimal("50.00"));
        inactiveProduct.setStockQuantity(0);
        inactiveProduct.setIsActive(false);

        Cart cart = new Cart();
        cart.setUser(activeUser);
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(inactiveProduct);
        item.setQuantity(1);
        cart.getItems().add(item);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponseDto result = cartService.removeItemFromCart(1L, 201L);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0L, result.getTotalItems());
    }

    @Test
    @DisplayName("Səbətdə olmayan məhsul yenilənəndə və ya silinəndə Azərbaycan dilində aydın xəta qaytarmalıdır")
    void updateAndRemove_whenProductNotInCart_shouldThrowEntityNotFoundException() {
        Cart cart = new Cart();
        cart.setUser(activeUser);
        cart.setItems(new ArrayList<>());

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        EntityNotFoundException exUpdate = assertThrows(
                EntityNotFoundException.class,
                () -> cartService.updateItemQuantity(1L, 999L, 2)
        );
        assertTrue(exUpdate.getMessage().contains("Məhsul səbətdə tapılmadı"));

        EntityNotFoundException exRemove = assertThrows(
                EntityNotFoundException.class,
                () -> cartService.removeItemFromCart(1L, 999L)
        );
        assertTrue(exRemove.getMessage().contains("Məhsul səbətdə tapılmadı"));
    }

    @Test
    @DisplayName("clearCart yalnız həmin səbətin CartItem-lərini təmizləməli, Cart, User və Product silinməməlidir")
    void clearCart_whenCartExists_shouldClearItemsWithoutDeletingCartOrUserOrProduct() {
        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(activeUser);

        CartItem item1 = new CartItem();
        item1.setId(1L);
        item1.setCart(cart);
        item1.setProduct(activeProduct);
        item1.setQuantity(2);
        cart.getItems().add(item1);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        cartService.clearCart(1L);

        assertTrue(cart.getItems().isEmpty(), "Səbətin elementləri təmizlənməlidir");
        assertNull(item1.getCart());

        verify(cartRepository).save(cart);

        // Product, User və Cart silinməməlidir!
        verify(cartRepository, never()).delete(any(Cart.class));
        verify(userRepository, never()).delete(any(User.class));
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("Səbət yoxdursa clearCart heç nə yaratmadan bitməlidir")
    void clearCart_whenCartDoesNotExist_shouldDoNothing() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        cartService.clearCart(1L);

        verify(cartRepository, never()).save(any());
        verify(cartRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Bir istifadəçinin əməliyyatları başqa istifadəçinin səbətinə və məhsul stokuna toxunmamalıdır")
    void writeOperations_shouldNeverModifyOtherUserCartOrProductStock() {
        // Digər istifadəçinin səbəti
        Cart otherCart = new Cart();
        otherCart.setId(20L);
        otherCart.setUser(otherUser);
        CartItem otherItem = new CartItem();
        otherItem.setId(88L);
        otherItem.setCart(otherCart);
        otherItem.setProduct(activeProduct);
        otherItem.setQuantity(5);
        otherCart.getItems().add(otherItem);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // User 1 səbətə əlavə edir
        cartService.addItemToCart(1L, new CartItemRequestDto(101L, 2));

        // Digər istifadəçinin səbətinin toxunulmaz qaldığını yoxla
        assertEquals(1, otherCart.getItems().size());
        assertEquals(5, otherItem.getQuantity());
        assertEquals(otherUser, otherCart.getUser());

        // Məhsulun stok miqdarının dəyişmədiyini yoxla
        assertEquals(10, activeProduct.getStockQuantity());
        verify(productRepository, never()).save(any(Product.class));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("Yazma metodlarında etibarsız parametrlər və null request rədd edilməlidir")
    void writeMethods_withInvalidCommonParameters_shouldThrowException() {
        // null request
        assertThrows(IllegalArgumentException.class, () -> cartService.addItemToCart(1L, null));

        // null və ya qeyri-müsbət userId
        assertThrows(IllegalArgumentException.class, () -> cartService.addItemToCart(null, new CartItemRequestDto(101L, 1)));
        assertThrows(IllegalArgumentException.class, () -> cartService.addItemToCart(0L, new CartItemRequestDto(101L, 1)));
        assertThrows(IllegalArgumentException.class, () -> cartService.addItemToCart(-1L, new CartItemRequestDto(101L, 1)));

        // null və ya qeyri-müsbət productId
        assertThrows(IllegalArgumentException.class, () -> cartService.addItemToCart(1L, new CartItemRequestDto(null, 1)));
        assertThrows(IllegalArgumentException.class, () -> cartService.addItemToCart(1L, new CartItemRequestDto(0L, 1)));
        assertThrows(IllegalArgumentException.class, () -> cartService.addItemToCart(1L, new CartItemRequestDto(-5L, 1)));

        // Deaktiv və ya tapılmayan istifadəçi
        when(userRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> cartService.addItemToCart(999L, new CartItemRequestDto(101L, 1)));

        when(userRepository.findByIdWithLock(2L)).thenReturn(Optional.of(inactiveUser));
        assertThrows(IllegalStateException.class, () -> cartService.addItemToCart(2L, new CartItemRequestDto(101L, 1)));
    }

    @Test
    @DisplayName("Stok 7 olduqda 10 ədəd əlavəetmə, kodla əlavəetmə və update rədd edilməlidir, səbət və stok qorunmalıdır")
    void stock7_quantity10_shouldRejectAndPreserveCartAndStock() {
        activeProduct.setStockQuantity(7);
        activeProduct.setCode("STK-777");

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));
        when(productRepository.findByCodeIgnoreCase("STK-777")).thenReturn(List.of(activeProduct));

        Cart cart = new Cart();
        cart.setUser(activeUser);
        cart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        // 1. ID ilə 10 əlavə etmək (stok 7) -> rədd
        IllegalArgumentException exAdd = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addItemToCart(1L, new CartItemRequestDto(101L, 10))
        );
        assertTrue(exAdd.getMessage().contains("mövcud stokdan (7) çox ola bilməz"));
        verify(cartRepository, never()).save(any());
        assertEquals(7, activeProduct.getStockQuantity());
        assertEquals(0, cart.getItems().size());

        // 2. Kodla 10 əlavə etmək (stok 7) -> rədd
        IllegalArgumentException exCode = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addItemByCode(1L, "STK-777", 10)
        );
        assertTrue(exCode.getMessage().contains("mövcud stokdan (7) çox ola bilməz"));
        verify(cartRepository, never()).save(any());
        assertEquals(7, activeProduct.getStockQuantity());
        assertEquals(0, cart.getItems().size());

        // 3. Səbətdə 2 var, 10-a update etmək istəyirik -> rədd
        CartItem item = new CartItem();
        item.setProduct(activeProduct);
        item.setQuantity(2);
        cart.getItems().add(item);

        IllegalArgumentException exUpdate = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateItemQuantity(1L, 101L, 10)
        );
        assertTrue(exUpdate.getMessage().contains("mövcud stokdan (7) çox ola bilməz"));
        assertEquals(2, item.getQuantity()); // Miqdar dəyişməz qalır
        assertEquals(7, activeProduct.getStockQuantity());
    }

    @Test
    @DisplayName("Səbətdə 5 var, əlavə tələb 3 olduqda cəm 8 > 7 rədd edilməlidir və səbət qorunmalıdır")
    void stock7_cartHas5_add3_shouldRejectAndPreserveCart() {
        activeProduct.setStockQuantity(7);

        Cart cart = new Cart();
        cart.setUser(activeUser);
        CartItem existingItem = new CartItem();
        existingItem.setProduct(activeProduct);
        existingItem.setQuantity(5);
        cart.getItems().add(existingItem);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addItemToCart(1L, new CartItemRequestDto(101L, 3))
        );

        assertTrue(ex.getMessage().contains("Tələb olunan miqdar (8) mövcud stokdan (7) çox ola bilməz"));
        verify(cartRepository, never()).save(any());
        assertEquals(5, existingItem.getQuantity()); // Səbətdəki 5 toxunulmaz qalır
        assertEquals(7, activeProduct.getStockQuantity());
    }

    @Test
    @DisplayName("CartItemResponseDto məhsulun stockQuantity dəyərini saxlayır")
    void cartItemResponseDto_shouldIncludeStockQuantity() {
        activeProduct.setStockQuantity(7);

        Cart cart = new Cart();
        cart.setUser(activeUser);
        CartItem item = new CartItem();
        item.setId(10L);
        item.setProduct(activeProduct);
        item.setQuantity(2);
        cart.getItems().add(item);

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartResponseDto response = cartService.getCart(1L);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(7, response.getItems().get(0).getStockQuantity());
        assertTrue(response.getItems().get(0).getAvailable());
    }

    @Test
    @DisplayName("Səbətə əlavə edərkən məhsul refresh xətası verdikdə əməliyyat dayanır və səbət saxlanmır")
    void addItemToCart_WhenRefreshFails_ShouldThrowExceptionAndNotModifyCart() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));
        doThrow(new jakarta.persistence.PersistenceException("DB refresh error"))
                .when(entityManager).refresh(any());

        CartItemRequestDto request = new CartItemRequestDto(101L, 2);

        assertThrows(jakarta.persistence.PersistenceException.class, () ->
                cartService.addItemToCart(1L, request)
        );

        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Səbətdə miqdar yenilənərkən məhsul refresh xətası verdikdə səbət saxlanmır və miqdar dəyişmir")
    void updateItemQuantity_WhenRefreshFails_ShouldThrowExceptionAndNotModifyCart() {
        Cart cart = new Cart();
        cart.setUser(activeUser);
        CartItem item = new CartItem();
        item.setId(10L);
        item.setProduct(activeProduct);
        item.setQuantity(2);
        cart.getItems().add(item);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(101L)).thenReturn(Optional.of(activeProduct));
        doThrow(new jakarta.persistence.PersistenceException("DB refresh error"))
                .when(entityManager).refresh(any());

        assertThrows(jakarta.persistence.PersistenceException.class, () ->
                cartService.updateItemQuantity(1L, 101L, 5)
        );

        verify(cartRepository, never()).save(any());
        assertEquals(2, item.getQuantity());
    }

    @Test
    @DisplayName("getCartTotalItems: Yanlış istifadəçi ID-si üçün IllegalArgumentException atılır")
    void getCartTotalItems_invalidUserId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cartService.getCartTotalItems(null));
        assertThrows(IllegalArgumentException.class, () -> cartService.getCartTotalItems(0L));
        assertThrows(IllegalArgumentException.class, () -> cartService.getCartTotalItems(-5L));
        verifyNoInteractions(userRepository, cartRepository);
    }

    @Test
    @DisplayName("getCartTotalItems: İstifadəçi tapılmadıqda EntityNotFoundException atılır")
    void getCartTotalItems_userNotFound_throwsEntityNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> cartService.getCartTotalItems(999L));
        verify(cartRepository, never()).sumQuantityByUserId(any());
    }

    @Test
    @DisplayName("getCartTotalItems: İstifadəçi deaktiv olduqda IllegalStateException atılır")
    void getCartTotalItems_userInactive_throwsIllegalStateException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(inactiveUser));

        assertThrows(IllegalStateException.class, () -> cartService.getCartTotalItems(2L));
        verify(cartRepository, never()).sumQuantityByUserId(any());
    }

    @Test
    @DisplayName("getCartTotalItems: Aktiv istifadəçi üçün DB səviyyəsində məhsul miqdarlarının cəmini qaytarır və entity yükləmir")
    void getCartTotalItems_activeUser_returnsSumQuantity() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.sumQuantityByUserId(1L)).thenReturn(14L);

        Long total = cartService.getCartTotalItems(1L);

        assertEquals(14L, total);
        verify(cartRepository, times(1)).sumQuantityByUserId(1L);
        // Entity və ya əlavə sorğular icra edilmir
        verify(cartRepository, never()).findByUserId(any());
        verifyNoInteractions(productRepository);
    }
}
