package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.response.OrderItemResponseDto;
import com.celiloglu.backend.dto.response.OrderResponseDto;
import com.celiloglu.backend.entity.Cart;
import com.celiloglu.backend.entity.CartItem;
import com.celiloglu.backend.entity.Order;
import com.celiloglu.backend.entity.OrderItem;
import com.celiloglu.backend.entity.Payment;
import com.celiloglu.backend.entity.Product;
import com.celiloglu.backend.entity.User;
import com.celiloglu.backend.enums.OrderStatus;
import com.celiloglu.backend.enums.PaymentMethod;
import com.celiloglu.backend.enums.PaymentStatus;
import com.celiloglu.backend.repository.CartRepository;
import com.celiloglu.backend.repository.OrderRepository;
import com.celiloglu.backend.repository.ProductRepository;
import com.celiloglu.backend.repository.UserRepository;
import com.celiloglu.backend.service.impl.OrderServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User activeUser;
    private User inactiveUser;
    private Product activeProduct;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setUsername("active_user");
        activeUser.setEmail("active@example.com");
        activeUser.setPassword("secret_hashed_password_123");
        activeUser.setIsActive(true);

        inactiveUser = new User();
        inactiveUser.setId(2L);
        inactiveUser.setUsername("inactive_user");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setIsActive(false);

        activeProduct = new Product();
        activeProduct.setId(101L);
        activeProduct.setName("Əyləc diski");
        activeProduct.setCode("BRK-001");
        activeProduct.setImageUrl("/product-images/brake.jpg");
        activeProduct.setPrice(new BigDecimal("250.00"));
        activeProduct.setStockQuantity(10);
        activeProduct.setIsActive(true);

        inactiveProduct = new Product();
        inactiveProduct.setId(102L);
        inactiveProduct.setName("Köhnə model amortizator");
        inactiveProduct.setCode("SHK-999");
        inactiveProduct.setImageUrl("/product-images/shock.jpg");
        inactiveProduct.setPrice(new BigDecimal("180.00"));
        inactiveProduct.setStockQuantity(5);
        inactiveProduct.setIsActive(false);
    }

    @Test
    @DisplayName("getUserOrders: Yanlış istifadəçi ID-si olduqda IllegalArgumentException atmalıdır")
    void getUserOrders_withInvalidUserId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> orderService.getUserOrders(null, PageRequest.of(0, 10)));
        assertThrows(IllegalArgumentException.class, () -> orderService.getUserOrders(0L, PageRequest.of(0, 10)));
        assertThrows(IllegalArgumentException.class, () -> orderService.getUserOrders(-1L, PageRequest.of(0, 10)));

        verifyNoInteractions(userRepository);
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("getUserOrders: İstifadəçi tapılmadıqda EntityNotFoundException atmalıdır")
    void getUserOrders_whenUserNotFound_throwsEntityNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> orderService.getUserOrders(99L, PageRequest.of(0, 10)));

        assertTrue(ex.getMessage().contains("İstifadəçi tapılmadı"));
        verify(userRepository).findById(99L);
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("getUserOrders: İstifadəçi deaktiv olduqda IllegalStateException atmalıdır")
    void getUserOrders_whenUserInactive_throwsIllegalStateException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(inactiveUser));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.getUserOrders(2L, PageRequest.of(0, 10)));

        assertEquals("İstifadəçi hesabı deaktivdir!", ex.getMessage());
        verify(userRepository).findById(2L);
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("getUserOrders: İstifadəçinin sifarişi olmadıqda boş Page qaytarmalıdır")
    void getUserOrders_whenOrdersEmpty_returnsEmptyPage() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(orderRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

        Page<OrderResponseDto> result = orderService.getUserOrders(1L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(orderRepository).findByUserId(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("getUserOrders: Səhifələmə DB-də aparılmalı və orderDate DESC, id DESC sırası təmin edilməlidir")
    void getUserOrders_enforcesDbSortingByOrderDateDescAndIdDesc() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        Order order1 = createSampleOrder(10L, "ORD-100", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("100.00"));
        when(orderRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order1), PageRequest.of(0, 10), 1));

        Page<OrderResponseDto> result = orderService.getUserOrders(1L, PageRequest.of(1, 5));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findByUserId(eq(1L), captor.capture());

        Pageable captured = captor.getValue();
        assertEquals(1, captured.getPageNumber());
        assertEquals(5, captured.getPageSize());

        Sort sort = captured.getSort();
        assertNotNull(sort);
        Sort.Order orderDateSort = sort.getOrderFor("orderDate");
        Sort.Order idSort = sort.getOrderFor("id");

        assertNotNull(orderDateSort, "orderDate üzrə sıralama olmalıdır");
        assertEquals(Sort.Direction.DESC, orderDateSort.getDirection(), "orderDate DESC olmalıdır");
        assertNotNull(idSort, "id üzrə sıralama olmalıdır");
        assertEquals(Sort.Direction.DESC, idSort.getDirection(), "id DESC olmalıdır");
    }

    @Test
    @DisplayName("getOrderDetails: Yanlış istifadəçi və ya sifariş ID-si verildikdə IllegalArgumentException atmalıdır")
    void getOrderDetails_withInvalidIds_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderDetails(null, 1L));
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderDetails(0L, 1L));
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderDetails(-1L, 1L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderDetails(1L, null));
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderDetails(1L, 0L));
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderDetails(1L, -1L));
    }

    @Test
    @DisplayName("getOrderDetails: İstifadəçi deaktiv olduqda IllegalStateException atmalıdır")
    void getOrderDetails_whenUserInactive_throwsIllegalStateException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(inactiveUser));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.getOrderDetails(2L, 10L));

        assertEquals("İstifadəçi hesabı deaktivdir!", ex.getMessage());
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("getOrderDetails: Mövcud olmayan sifariş axtarıldıqda dəqiq 'Sifariş tapılmadı!' xətası atmalıdır")
    void getOrderDetails_whenOrderNotFound_throwsEntityNotFoundExceptionWithStandardMessage() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(orderRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> orderService.getOrderDetails(1L, 999L));

        assertEquals("Sifariş tapılmadı!", ex.getMessage());
        verify(orderRepository).findByIdAndUserId(999L, 1L);
    }

    @Test
    @DisplayName("getOrderDetails: Başqa istifadəçiyə aid sifariş axtarıldıqda eyni 'Sifariş tapılmadı!' xətası atmalıdır")
    void getOrderDetails_whenOrderBelongsToAnotherUser_throwsEntityNotFoundExceptionWithSameMessage() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(orderRepository.findByIdAndUserId(50L, 1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> orderService.getOrderDetails(1L, 50L));

        assertEquals("Sifariş tapılmadı!", ex.getMessage());
        verify(orderRepository).findByIdAndUserId(50L, 1L);
    }

    @Test
    @DisplayName("getOrderDetails: Məhsulun cari qiyməti dəyişsə belə satınalma anındakı priceAtPurchase ilə hesablama aparılmalıdır")
    void getOrderDetails_calculatesSubtotalUsingPriceAtPurchaseEvenIfProductCurrentPriceChanged() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.HAZIRLANIR, new BigDecimal("270.00"));

        OrderItem item1 = new OrderItem();
        item1.setId(1001L);
        item1.setOrder(order);
        item1.setProduct(activeProduct);
        item1.setQuantity(3);
        item1.setPriceAtPurchase(new BigDecimal("70.00"));

        OrderItem item2 = new OrderItem();
        item2.setId(1002L);
        item2.setOrder(order);
        item2.setProduct(inactiveProduct);
        item2.setQuantity(2);
        item2.setPriceAtPurchase(new BigDecimal("30.00"));

        List<OrderItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);
        order.setItems(items);

        when(orderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));

        OrderResponseDto result = orderService.getOrderDetails(1L, 10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("ORD-12345", result.getOrderNumber());
        assertEquals(OrderStatus.HAZIRLANIR, result.getStatus());
        assertEquals(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR, result.getPaymentStatus());
        assertEquals(PaymentMethod.NAĞD, result.getPaymentMethod());

        assertEquals(5L, result.getTotalItems());
        assertEquals(new BigDecimal("270.00"), result.getTotalAmount());

        assertEquals(2, result.getItems().size());

        OrderItemResponseDto dto1 = result.getItems().get(0);
        assertEquals(1001L, dto1.getId());
        assertEquals(101L, dto1.getProductId());
        assertEquals("Əyləc diski", dto1.getProductName());
        assertEquals("BRK-001", dto1.getProductCode());
        assertEquals("/product-images/brake.jpg", dto1.getImageUrl());
        assertEquals(3, dto1.getQuantity());
        assertEquals(new BigDecimal("70.00"), dto1.getPriceAtPurchase());
        assertEquals(new BigDecimal("210.00"), dto1.getSubtotal());

        OrderItemResponseDto dto2 = result.getItems().get(1);
        assertEquals(1002L, dto2.getId());
        assertEquals(102L, dto2.getProductId());
        assertEquals("Köhnə model amortizator", dto2.getProductName());
        assertEquals("SHK-999", dto2.getProductCode());
        assertEquals(2, dto2.getQuantity());
        assertEquals(new BigDecimal("30.00"), dto2.getPriceAtPurchase());
        assertEquals(new BigDecimal("60.00"), dto2.getSubtotal());
    }

    @Test
    @DisplayName("mapToOrderResponseDto: GÖZLƏYİR, ÖDƏNİŞ_GÖZLƏYİR və ödənişsiz sifarişdə canCancel true olmalıdır")
    void mapToOrderResponseDto_canCancel_isTrueWhenEligible() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("100.00"));
        order.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);
        order.setPayments(new ArrayList<>());

        when(orderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));

        OrderResponseDto dto = orderService.getOrderDetails(1L, 10L);

        assertNotNull(dto);
        assertTrue(dto.isCanCancel());
    }

    @Test
    @DisplayName("mapToOrderResponseDto: Sifariş statusu GÖZLƏYİR deyilsə canCancel false olmalıdır")
    void mapToOrderResponseDto_canCancel_isFalseWhenOrderStatusNotPending() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.HAZIRLANIR, new BigDecimal("100.00"));
        order.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);

        when(orderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));

        OrderResponseDto dto = orderService.getOrderDetails(1L, 10L);

        assertNotNull(dto);
        assertFalse(dto.isCanCancel());
    }

    @Test
    @DisplayName("mapToOrderResponseDto: Ödəniş statusu ÖDƏNİŞ_GÖZLƏYİR deyilsə canCancel false olmalıdır")
    void mapToOrderResponseDto_canCancel_isFalseWhenPaymentStatusNotPending() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("100.00"));
        order.setPaymentStatus(PaymentStatus.TAM_ÖDƏNİLMİŞ);

        when(orderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));

        OrderResponseDto dto = orderService.getOrderDetails(1L, 10L);

        assertNotNull(dto);
        assertFalse(dto.isCanCancel());
    }

    @Test
    @DisplayName("mapToOrderResponseDto: Sifarişə aid ödəniş qeydi varsa canCancel false olmalıdır")
    void mapToOrderResponseDto_canCancel_isFalseWhenOrderHasPayments() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("100.00"));
        order.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);
        Payment payment = new Payment();
        payment.setId(99L);
        order.getPayments().add(payment);

        when(orderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));

        OrderResponseDto dto = orderService.getOrderDetails(1L, 10L);

        assertNotNull(dto);
        assertFalse(dto.isCanCancel());
    }

    // ==========================================
    // createOrderFromCart TESTLƏRİ
    // ==========================================

    @Test
    @DisplayName("createOrderFromCart: Uğurlu sifariş, qiymət snapshot-u, məbləğ hesablanması, stokun azalması və səbətin təmizlənməsi")
    void createOrderFromCart_successfulOrder_createsSnapshotDeductsStockClearsCart() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Cart cart = new Cart();
        cart.setId(500L);
        cart.setUser(activeUser);

        CartItem item1 = new CartItem();
        item1.setId(10L);
        item1.setCart(cart);
        item1.setProduct(activeProduct);
        item1.setQuantity(2);

        Product secondProduct = new Product();
        secondProduct.setId(103L);
        secondProduct.setName("Yağ filtri");
        secondProduct.setCode("FLT-003");
        secondProduct.setPrice(new BigDecimal("40.00"));
        secondProduct.setStockQuantity(5);
        secondProduct.setIsActive(true);

        CartItem item2 = new CartItem();
        item2.setId(11L);
        item2.setCart(cart);
        item2.setProduct(secondProduct);
        item2.setQuantity(1);

        cart.getItems().add(item1);
        cart.getItems().add(item2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(activeProduct));
        when(productRepository.findByIdWithLock(103L)).thenReturn(Optional.of(secondProduct));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(888L);
            return o;
        });

        OrderResponseDto response = orderService.createOrderFromCart(
                1L, "  Nərimanov ray., Ağa Neymətulla küç. 12  ", "  Zəhmət olmasa zəng edin  ");

        assertNotNull(response);
        assertEquals(888L, response.getId());
        assertTrue(response.getOrderNumber().startsWith("ORD-"));
        assertEquals(OrderStatus.GÖZLƏYİR, response.getStatus());
        assertEquals(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR, response.getPaymentStatus());
        assertEquals(PaymentMethod.NAĞD, response.getPaymentMethod());
        assertEquals("Nərimanov ray., Ağa Neymətulla küç. 12", response.getShippingAddress());
        assertEquals("Zəhmət olmasa zəng edin", response.getOrderNotes());

        // Məbləğ: 2 * 250.00 + 1 * 40.00 = 540.00 AZN
        assertEquals(new BigDecimal("540.00"), response.getTotalAmount());
        assertEquals(3L, response.getTotalItems());
        assertEquals(2, response.getItems().size());

        // Stok yoxlaması: 101L (10 - 2 = 8), 103L (5 - 1 = 4)
        assertEquals(8, activeProduct.getStockQuantity());
        assertEquals(4, secondProduct.getStockQuantity());
        verify(productRepository).save(activeProduct);
        verify(productRepository).save(secondProduct);

        // Səbət təmizlənməsi yoxlaması
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("createOrderFromCart: Məhsul kilidləri həmişə productId artan sıra ilə alınmalıdır")
    void createOrderFromCart_locksProductsInAscendingIdOrder() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Cart cart = new Cart();
        cart.setId(500L);
        cart.setUser(activeUser);

        Product p105 = new Product();
        p105.setId(105L);
        p105.setName("P105");
        p105.setPrice(new BigDecimal("10.00"));
        p105.setStockQuantity(10);
        p105.setIsActive(true);

        Product p101 = new Product();
        p101.setId(101L);
        p101.setName("P101");
        p101.setPrice(new BigDecimal("20.00"));
        p101.setStockQuantity(10);
        p101.setIsActive(true);

        Product p103 = new Product();
        p103.setId(103L);
        p103.setName("P103");
        p103.setPrice(new BigDecimal("30.00"));
        p103.setStockQuantity(10);
        p103.setIsActive(true);

        // Səbətdə qarışıq ardıcıllıqla yerləşdirilib: 105, 101, 103
        CartItem i1 = new CartItem();
        i1.setProduct(p105);
        i1.setQuantity(1);
        i1.setCart(cart);
        CartItem i2 = new CartItem();
        i2.setProduct(p101);
        i2.setQuantity(1);
        i2.setCart(cart);
        CartItem i3 = new CartItem();
        i3.setProduct(p103);
        i3.setQuantity(1);
        i3.setCart(cart);

        cart.getItems().addAll(List.of(i1, i2, i3));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(p101));
        when(productRepository.findByIdWithLock(103L)).thenReturn(Optional.of(p103));
        when(productRepository.findByIdWithLock(105L)).thenReturn(Optional.of(p105));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrderFromCart(1L, null, null);

        // Yoxlayırıq ki, kilidlər 101 -> 103 -> 105 sırası ilə alınıb
        InOrder inOrder = inOrder(productRepository);
        inOrder.verify(productRepository).findByIdWithLock(101L);
        inOrder.verify(productRepository).findByIdWithLock(103L);
        inOrder.verify(productRepository).findByIdWithLock(105L);
    }

    @Test
    @DisplayName("createOrderFromCart: Eyni məhsul bir neçə sətirdədirsə miqdarları cəmlənməli və ümumi stokla yoxlanmalıdır")
    void createOrderFromCart_aggregatesQuantitiesForSameProductLinesWithoutOverflow() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Cart cart = new Cart();
        cart.setId(500L);
        cart.setUser(activeUser);

        // activeProduct stokunda 10 ədəd var
        // Səbətdə iki sətirdə eyni məhsul: 6 ədəd + 5 ədəd = 11 ədəd (stokdan çoxdur!)
        CartItem i1 = new CartItem();
        i1.setProduct(activeProduct);
        i1.setQuantity(6);
        i1.setCart(cart);
        CartItem i2 = new CartItem();
        i2.setProduct(activeProduct);
        i2.setQuantity(5);
        i2.setCart(cart);

        cart.getItems().addAll(List.of(i1, i2));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(activeProduct));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrderFromCart(1L, null, null));

        assertTrue(ex.getMessage().contains("Tələb olunan miqdar (11) mövcud stokdan (10) çox ola bilməz"));

        // Heç bir dəyişiklik edilməməlidir
        assertEquals(10, activeProduct.getStockQuantity());
        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        assertEquals(2, cart.getItems().size());
    }

    @Test
    @DisplayName("createOrderFromCart: Səbət boş olduqda və ya tapılmadıqda IllegalStateException atmalıdır")
    void createOrderFromCart_whenCartEmptyOrNull_throwsIllegalStateException() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        IllegalStateException ex1 = assertThrows(IllegalStateException.class,
                () -> orderService.createOrderFromCart(1L, null, null));
        assertEquals("Səbət boşdur!", ex1.getMessage());

        Cart emptyCart = new Cart();
        emptyCart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(emptyCart));

        IllegalStateException ex2 = assertThrows(IllegalStateException.class,
                () -> orderService.createOrderFromCart(1L, null, null));
        assertEquals("Səbət boşdur!", ex2.getMessage());
    }

    @Test
    @DisplayName("createOrderFromCart: İstifadəçi deaktiv olduqda IllegalStateException atmalıdır")
    void createOrderFromCart_whenUserInactive_throwsIllegalStateException() {
        when(userRepository.findByIdWithLock(2L)).thenReturn(Optional.of(inactiveUser));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.createOrderFromCart(2L, null, null));
        assertEquals("İstifadəçi hesabı deaktivdir!", ex.getMessage());

        verifyNoInteractions(cartRepository);
        verifyNoInteractions(productRepository);
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("createOrderFromCart: Deaktiv məhsul olduqda sifariş rədd edilməli və stok/səbət dəyişməməlidir")
    void createOrderFromCart_whenProductInactive_throwsIllegalStateExceptionAndNoChanges() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Cart cart = new Cart();
        cart.setId(500L);
        cart.setUser(activeUser);

        CartItem item = new CartItem();
        item.setProduct(inactiveProduct);
        item.setQuantity(1);
        item.setCart(cart);
        cart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(102L)).thenReturn(Optional.of(inactiveProduct));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.createOrderFromCart(1L, null, null));

        assertTrue(ex.getMessage().contains("Məhsul deaktivdir və sifariş edilə bilməz"));
        assertEquals(5, inactiveProduct.getStockQuantity());
        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        assertEquals(1, cart.getItems().size());
    }

    @Test
    @DisplayName("createOrderFromCart: Məhsulun qiyməti sıfır və ya mənfi olduqda IllegalStateException atmalıdır")
    void createOrderFromCart_whenProductPriceZeroOrNegative_throwsIllegalStateException() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Cart cart = new Cart();
        cart.setId(500L);
        cart.setUser(activeUser);

        Product invalidPriceProduct = new Product();
        invalidPriceProduct.setId(201L);
        invalidPriceProduct.setName("Havasız təkər");
        invalidPriceProduct.setPrice(BigDecimal.ZERO);
        invalidPriceProduct.setStockQuantity(5);
        invalidPriceProduct.setIsActive(true);

        CartItem item = new CartItem();
        item.setProduct(invalidPriceProduct);
        item.setQuantity(1);
        item.setCart(cart);
        cart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(201L)).thenReturn(Optional.of(invalidPriceProduct));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.createOrderFromCart(1L, null, null));

        assertTrue(ex.getMessage().contains("Məhsulun qiyməti düzgün deyil"));
        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrderFromCart: Məhsul sayı 0 və ya mənfi olduqda IllegalArgumentException atmalıdır")
    void createOrderFromCart_whenQuantityInvalid_throwsIllegalArgumentException() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Cart cart = new Cart();
        cart.setId(500L);
        cart.setUser(activeUser);

        CartItem item = new CartItem();
        item.setProduct(activeProduct);
        item.setQuantity(0);
        item.setCart(cart);
        cart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrderFromCart(1L, null, null));

        assertEquals("Məhsul sayı ən azı 1 olmalıdır!", ex.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrderFromCart: Stok tələb olunan saydan az olduqda sifariş rədd edilməlidir")
    void createOrderFromCart_whenInsufficientStock_throwsIllegalArgumentException() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Cart cart = new Cart();
        cart.setId(500L);
        cart.setUser(activeUser);

        CartItem item = new CartItem();
        item.setProduct(activeProduct);
        item.setQuantity(15); // Stokda cəmi 10 var
        item.setCart(cart);
        cart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(activeProduct));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrderFromCart(1L, null, null));

        assertTrue(ex.getMessage().contains("mövcud stokdan (10) çox ola bilməz"));
        assertEquals(10, activeProduct.getStockQuantity());
        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrderFromCart: Sifariş bazaya saxlanarkən xəta baş verərsə, xəta udulmamalıdır")
    void createOrderFromCart_whenOrderSaveThrowsException_propagatesException() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Cart cart = new Cart();
        cart.setId(500L);
        cart.setUser(activeUser);

        CartItem item = new CartItem();
        item.setProduct(activeProduct);
        item.setQuantity(1);
        item.setCart(cart);
        cart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(activeProduct));
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("Baza əlaqə xətası!"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.createOrderFromCart(1L, null, null));

        assertEquals("Baza əlaqə xətası!", ex.getMessage());
    }

    @Test
    @DisplayName("createOrderFromCart: Başqa istifadəçinin səbətinə toxunulmamalıdır")
    void createOrderFromCart_doesNotTouchOtherUserCart() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Cart userCart = new Cart();
        userCart.setId(501L);
        userCart.setUser(activeUser);

        CartItem userItem = new CartItem();
        userItem.setProduct(activeProduct);
        userItem.setQuantity(1);
        userItem.setCart(userCart);
        userCart.getItems().add(userItem);

        Cart otherUserCart = new Cart();
        otherUserCart.setId(502L);
        User otherUser = new User();
        otherUser.setId(99L);
        otherUserCart.setUser(otherUser);

        CartItem otherItem = new CartItem();
        otherItem.setProduct(activeProduct);
        otherItem.setQuantity(4);
        otherItem.setCart(otherUserCart);
        otherUserCart.getItems().add(otherItem);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(userCart));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(activeProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.createOrderFromCart(1L, null, null);

        // Yalnız cari istifadəçinin səbəti boşaldılır
        assertTrue(userCart.getItems().isEmpty());
        // Başqa istifadəçinin səbəti tam toxunulmaz qalır
        assertEquals(1, otherUserCart.getItems().size());
        assertEquals(4, otherUserCart.getItems().get(0).getQuantity());
    }

    @Test
    @DisplayName("createOrderFromCart: Ünvan və qeyd 255 simvoldan çox olduqda IllegalArgumentException atmalıdır")
    void createOrderFromCart_sanitizesAndValidatesAddressAndNotes() {
        String longText = "A".repeat(256);

        IllegalArgumentException exAddress = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrderFromCart(1L, longText, "Qeyd"));
        assertEquals("Çatdırılma ünvanı maksimum 255 simvol ola bilər!", exAddress.getMessage());

        IllegalArgumentException exNotes = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrderFromCart(1L, "Bakı", longText));
        assertEquals("Sifariş qeydi maksimum 255 simvol ola bilər!", exNotes.getMessage());
    }

    @Test
    @DisplayName("createOrderFromCart: Stok dəqiq sifariş qədər azaldıqda 0 ola bilər")
    void createOrderFromCart_allowsStockToBecomeZero() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Cart cart = new Cart();
        cart.setId(500L);
        cart.setUser(activeUser);

        CartItem item = new CartItem();
        item.setProduct(activeProduct);
        item.setQuantity(10); // Stok 10-dur, hamısı sifariş edilir
        item.setCart(cart);
        cart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(activeProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponseDto response = orderService.createOrderFromCart(1L, null, null);

        assertNotNull(response);
        assertEquals(0, activeProduct.getStockQuantity());
        verify(productRepository).save(activeProduct);
    }

    @Test
    @DisplayName("createOrderFromCart: Stok 7, sifariş miqdarı 10 olduqda tam rədd edilir, səbət və stok qorunur")
    void createOrderFromCart_whenStock7AndOrderQty10_shouldRejectAndPreserveCartAndStock() {
        activeProduct.setStockQuantity(7);
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Cart cart = new Cart();
        cart.setId(500L);
        cart.setUser(activeUser);

        CartItem item = new CartItem();
        item.setProduct(activeProduct);
        item.setQuantity(10);
        item.setCart(cart);
        cart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(activeProduct));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrderFromCart(1L, null, null)
        );

        assertTrue(ex.getMessage().contains("Tələb olunan miqdar (10) mövcud stokdan (7) çox ola bilməz"));
        verify(orderRepository, never()).save(any());
        verify(cartRepository, never()).save(any());
        assertEquals(7, activeProduct.getStockQuantity());
        assertEquals(1, cart.getItems().size());
        assertEquals(10, cart.getItems().get(0).getQuantity());
    }

    @Test
    @DisplayName("createOrderFromCart: Stok 7, sifariş 7 olduqda uğurla tamamlanır və qalan stok 0 olur")
    void createOrderFromCart_whenStock7AndOrderQty7_shouldSucceedAndLeaveStock0() {
        activeProduct.setStockQuantity(7);
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Cart cart = new Cart();
        cart.setId(500L);
        cart.setUser(activeUser);

        CartItem item = new CartItem();
        item.setProduct(activeProduct);
        item.setQuantity(7);
        item.setCart(cart);
        cart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(activeProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponseDto response = orderService.createOrderFromCart(1L, null, null);

        assertNotNull(response);
        assertEquals(0, activeProduct.getStockQuantity());
        verify(productRepository).save(activeProduct);
        verify(orderRepository).save(any(Order.class));
        assertTrue(cart.getItems().isEmpty());
    }

    // ==========================================
    // cancelOrder TESTLƏRİ
    // ==========================================

    @Test
    @DisplayName("cancelOrder: Yanlış istifadəçi və ya sifariş ID-si verildikdə IllegalArgumentException atmalıdır")
    void cancelOrder_withInvalidIds_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> orderService.cancelOrder(null, 10L));
        assertThrows(IllegalArgumentException.class, () -> orderService.cancelOrder(0L, 10L));
        assertThrows(IllegalArgumentException.class, () -> orderService.cancelOrder(-1L, 10L));

        assertThrows(IllegalArgumentException.class, () -> orderService.cancelOrder(1L, null));
        assertThrows(IllegalArgumentException.class, () -> orderService.cancelOrder(1L, 0L));
        assertThrows(IllegalArgumentException.class, () -> orderService.cancelOrder(1L, -10L));

        verifyNoInteractions(userRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("cancelOrder: İstifadəçi tapılmadıqda EntityNotFoundException atmalıdır")
    void cancelOrder_whenUserNotFound_throwsEntityNotFoundException() {
        when(userRepository.findByIdWithLock(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> orderService.cancelOrder(99L, 10L));

        assertTrue(ex.getMessage().contains("İstifadəçi tapılmadı"));
        verify(userRepository).findByIdWithLock(99L);
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("cancelOrder: İstifadəçi deaktiv olduqda IllegalStateException atmalıdır")
    void cancelOrder_whenUserInactive_throwsIllegalStateException() {
        when(userRepository.findByIdWithLock(2L)).thenReturn(Optional.of(inactiveUser));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.cancelOrder(2L, 10L));

        assertEquals("İstifadəçi hesabı deaktivdir!", ex.getMessage());
        verify(userRepository).findByIdWithLock(2L);
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("cancelOrder: Sifariş tapılmadıqda və ya başqa istifadəçiyə aid olduqda EntityNotFoundException('Sifariş tapılmadı!') atmalıdır")
    void cancelOrder_whenOrderNotFoundOrBelongsToAnotherUser_throwsEntityNotFoundException() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(orderRepository.findByIdAndUserIdWithLock(10L, 1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> orderService.cancelOrder(1L, 10L));

        assertEquals("Sifariş tapılmadı!", ex.getMessage());
        verify(orderRepository).findByIdAndUserIdWithLock(10L, 1L);
        verifyNoInteractions(productRepository);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelOrder: Sifariş artıq LƏĞV_EDİLDİ vəziyyətindədirsə, idempotent olaraq heç bir dəyişiklik etmədən tamamlanmalıdır")
    void cancelOrder_whenAlreadyCancelled_completesCleanlyWithoutChanges() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.LƏĞV_EDİLDİ, new BigDecimal("100.00"));
        when(orderRepository.findByIdAndUserIdWithLock(10L, 1L)).thenReturn(Optional.of(order));

        assertDoesNotThrow(() -> orderService.cancelOrder(1L, 10L));

        // Stok və sifariş dəyişdirilməməlidir
        verifyNoInteractions(productRepository);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelOrder: Sifariş statusu GÖZLƏYİR deyilsə (məs. HAZIRLANIR, ÇATDIRILDI) IllegalStateException atmalıdır")
    void cancelOrder_whenOrderStatusNotPending_throwsIllegalStateException() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.HAZIRLANIR, new BigDecimal("100.00"));
        when(orderRepository.findByIdAndUserIdWithLock(10L, 1L)).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.cancelOrder(1L, 10L));

        assertTrue(ex.getMessage().contains("Yalnız 'Gözləyir' statusunda olan sifarişlər ləğv edilə bilər"));
        verifyNoInteractions(productRepository);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelOrder: Ödəniş statusu ÖDƏNİŞ_GÖZLƏYİR deyilsə (məs. TAM_ÖDƏNİLMİŞ) IllegalStateException atmalıdır")
    void cancelOrder_whenPaymentStatusNotPending_throwsIllegalStateException() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("100.00"));
        order.setPaymentStatus(PaymentStatus.TAM_ÖDƏNİLMİŞ);
        when(orderRepository.findByIdAndUserIdWithLock(10L, 1L)).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.cancelOrder(1L, 10L));

        assertTrue(ex.getMessage().contains("Ödənişi başlanmış və ya tamamlanmış sifarişlər ləğv edilə bilməz"));
        verifyNoInteractions(productRepository);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelOrder: Sifarişə aid ödəniş qeydi varsa IllegalStateException atmalıdır")
    void cancelOrder_whenOrderHasPaymentRecord_throwsIllegalStateException() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("100.00"));
        Payment payment = new Payment();
        payment.setId(55L);
        payment.setOrder(order);
        order.getPayments().add(payment);

        when(orderRepository.findByIdAndUserIdWithLock(10L, 1L)).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.cancelOrder(1L, 10L));

        assertTrue(ex.getMessage().contains("Ödəniş qeydi olan sifariş bu bölmədən ləğv edilə bilməz"));
        verifyNoInteractions(productRepository);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelOrder: Uğurlu ləğvetmədə stok bərpa edilməli, status LƏĞV_EDİLDİ olmalı və sifariş yadda saxlanmalıdır")
    void cancelOrder_successfulCancellation_restoresStockAndCancelsOrder() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("250.00"));

        activeProduct.setStockQuantity(8);

        Product secondProduct = new Product();
        secondProduct.setId(103L);
        secondProduct.setName("Hava filtri");
        secondProduct.setStockQuantity(4);
        secondProduct.setIsActive(true);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrder(order);
        item1.setProduct(activeProduct);
        item1.setQuantity(2);

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrder(order);
        item2.setProduct(secondProduct);
        item2.setQuantity(3);

        order.getItems().add(item1);
        order.getItems().add(item2);

        when(orderRepository.findByIdAndUserIdWithLock(10L, 1L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(activeProduct));
        when(productRepository.findByIdWithLock(103L)).thenReturn(Optional.of(secondProduct));

        orderService.cancelOrder(1L, 10L);

        // Stok bərpası yoxlanışı: 8 + 2 = 10, 4 + 3 = 7
        assertEquals(10, activeProduct.getStockQuantity());
        assertEquals(7, secondProduct.getStockQuantity());
        verify(productRepository).save(activeProduct);
        verify(productRepository).save(secondProduct);

        // Sifariş statusu yoxlanışı
        assertEquals(OrderStatus.LƏĞV_EDİLDİ, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("cancelOrder: Deadlock qorunması - məhsul kilidləri artan productId ardıcıllığı ilə alınmalıdır")
    void cancelOrder_locksProductsInAscendingIdOrder() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("600.00"));

        Product p105 = new Product();
        p105.setId(105L);
        p105.setName("P105");
        p105.setStockQuantity(5);
        p105.setIsActive(true);

        Product p101 = new Product();
        p101.setId(101L);
        p101.setName("P101");
        p101.setStockQuantity(5);
        p101.setIsActive(true);

        Product p103 = new Product();
        p103.setId(103L);
        p103.setName("P103");
        p103.setStockQuantity(5);
        p103.setIsActive(true);

        // Qarışıq ardıcıllıq: 105, 101, 103
        OrderItem i1 = new OrderItem();
        i1.setProduct(p105);
        i1.setQuantity(1);
        OrderItem i2 = new OrderItem();
        i2.setProduct(p101);
        i2.setQuantity(1);
        OrderItem i3 = new OrderItem();
        i3.setProduct(p103);
        i3.setQuantity(1);

        order.getItems().addAll(List.of(i1, i2, i3));

        when(orderRepository.findByIdAndUserIdWithLock(10L, 1L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(p101));
        when(productRepository.findByIdWithLock(103L)).thenReturn(Optional.of(p103));
        when(productRepository.findByIdWithLock(105L)).thenReturn(Optional.of(p105));

        orderService.cancelOrder(1L, 10L);

        InOrder inOrder = inOrder(productRepository);
        inOrder.verify(productRepository).findByIdWithLock(101L);
        inOrder.verify(productRepository).findByIdWithLock(103L);
        inOrder.verify(productRepository).findByIdWithLock(105L);
    }

    @Test
    @DisplayName("cancelOrder: Deaktiv məhsulun da stoku bərpa edilməli, lakin isActive dəyişdirilməməlidir")
    void cancelOrder_whenProductInactive_stillRestoresStockWithoutActivating() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("100.00"));

        inactiveProduct.setStockQuantity(5);
        assertFalse(inactiveProduct.getIsActive());

        OrderItem item = new OrderItem();
        item.setProduct(inactiveProduct);
        item.setQuantity(3);
        order.getItems().add(item);

        when(orderRepository.findByIdAndUserIdWithLock(10L, 1L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithLock(102L)).thenReturn(Optional.of(inactiveProduct));

        orderService.cancelOrder(1L, 10L);

        assertEquals(8, inactiveProduct.getStockQuantity());
        assertFalse(inactiveProduct.getIsActive(), "Məhsulun isActive statusu dəyişdirilməməlidir");
        verify(productRepository).save(inactiveProduct);
        assertEquals(OrderStatus.LƏĞV_EDİLDİ, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("cancelOrder: Eyni məhsul bir neçə sətirdədirsə miqdarları təhlükəsiz cəmlənməlidir")
    void cancelOrder_aggregatesQuantitiesForMultipleItemsOfSameProduct() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("100.00"));

        activeProduct.setStockQuantity(10);

        OrderItem item1 = new OrderItem();
        item1.setProduct(activeProduct);
        item1.setQuantity(3);

        OrderItem item2 = new OrderItem();
        item2.setProduct(activeProduct);
        item2.setQuantity(4);

        order.getItems().addAll(List.of(item1, item2));

        when(orderRepository.findByIdAndUserIdWithLock(10L, 1L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(activeProduct));

        orderService.cancelOrder(1L, 10L);

        // 10 + (3 + 4) = 17
        assertEquals(17, activeProduct.getStockQuantity());
        verify(productRepository, times(1)).save(activeProduct);
        assertEquals(OrderStatus.LƏĞV_EDİLDİ, order.getStatus());
    }

    @Test
    @DisplayName("cancelOrder: Stok overflow olduqda (currentStock + returnQty > Integer.MAX_VALUE) rədd edilməli və heç bir dəyişiklik edilməməlidir")
    void cancelOrder_whenStockOverflows_throwsIllegalArgumentExceptionAndPreservesState() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("100.00"));

        activeProduct.setStockQuantity(Integer.MAX_VALUE - 5);

        OrderItem item = new OrderItem();
        item.setProduct(activeProduct);
        item.setQuantity(10); // MAX_VALUE - 5 + 10 exceeds Integer.MAX_VALUE
        order.getItems().add(item);

        when(orderRepository.findByIdAndUserIdWithLock(10L, 1L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(activeProduct));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.cancelOrder(1L, 10L));

        assertTrue(ex.getMessage().contains("Stok miqdarı maksimum həddi aşır"));
        assertEquals(Integer.MAX_VALUE - 5, activeProduct.getStockQuantity());
        verify(productRepository, never()).save(any());
        assertEquals(OrderStatus.GÖZLƏYİR, order.getStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelOrder: Sifariş maddəsinin sayı < 1 olduqda IllegalArgumentException atmalıdır")
    void cancelOrder_whenOrderItemQuantityInvalid_throwsIllegalArgumentException() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("100.00"));

        OrderItem item = new OrderItem();
        item.setProduct(activeProduct);
        item.setQuantity(0);
        order.getItems().add(item);

        when(orderRepository.findByIdAndUserIdWithLock(10L, 1L)).thenReturn(Optional.of(order));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.cancelOrder(1L, 10L));

        assertTrue(ex.getMessage().contains("Məhsul sayı ən azı 1 olmalıdır"));
        verifyNoInteractions(productRepository);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelOrder: Sifariş maddəsində məhsul tapılmadıqda EntityNotFoundException atmalıdır")
    void cancelOrder_whenProductNotFound_throwsEntityNotFoundException() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));

        Order order = createSampleOrder(10L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("100.00"));

        OrderItem item = new OrderItem();
        item.setProduct(activeProduct);
        item.setQuantity(2);
        order.getItems().add(item);

        when(orderRepository.findByIdAndUserIdWithLock(10L, 1L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> orderService.cancelOrder(1L, 10L));

        assertTrue(ex.getMessage().contains("Məhsul tapılmadı! ID: 101"));
        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    // ==========================================
    // ADMIN SİFARİŞ SERVİS TESTLƏRİ
    // ==========================================

    @Test
    @DisplayName("getAllOrdersForAdmin: Fərqli müştərilərin sifarişlərini qaytarmalı və DB səviyyəsində orderDate DESC, id DESC sıralamasını tətbiq etməlidir")
    void getAllOrdersForAdmin_returnsOrdersFromMultipleCustomersWithDbPaginationAndSorting() {
        User user1 = new User();
        user1.setId(10L);
        user1.setUsername("musteri_bir");
        user1.setFirstName("Rəşad");
        user1.setLastName("Əliyev");
        user1.setEmail("resad@example.com");
        user1.setPhone("+994501112233");
        user1.setCompanyName("AzərTex MMC");

        User user2 = new User();
        user2.setId(20L);
        user2.setUsername("musteri_iki");
        user2.setFirstName("Leyla");
        user2.setLastName("Məmmədova");
        user2.setEmail("leyla@example.com");

        Order order1 = createSampleOrder(101L, "ORD-101", user1, OrderStatus.GÖZLƏYİR, new BigDecimal("150.00"));
        Order order2 = createSampleOrder(102L, "ORD-102", user2, OrderStatus.HAZIRLANIR, new BigDecimal("320.00"));

        Page<Order> orderPage = new PageImpl<>(List.of(order1, order2), PageRequest.of(1, 5), 2);
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(orderPage);

        Page<OrderResponseDto> result = orderService.getAllOrdersForAdmin(PageRequest.of(1, 5));

        assertNotNull(result);
        assertEquals(2, result.getContent().size());

        // Müştəri 1 məlumatlarının DTO-ya çıxması
        OrderResponseDto dto1 = result.getContent().get(0);
        assertEquals(101L, dto1.getId());
        assertEquals("ORD-101", dto1.getOrderNumber());
        assertEquals(10L, dto1.getCustomerId());
        assertEquals("musteri_bir", dto1.getCustomerUsername());
        assertEquals("Rəşad Əliyev", dto1.getCustomerFullName());
        assertEquals("AzərTex MMC", dto1.getCustomerCompanyName());
        assertEquals("resad@example.com", dto1.getCustomerEmail());
        assertEquals("+994501112233", dto1.getCustomerPhone());

        // Müştəri 2 məlumatlarının DTO-ya çıxması
        OrderResponseDto dto2 = result.getContent().get(1);
        assertEquals(102L, dto2.getId());
        assertEquals("ORD-102", dto2.getOrderNumber());
        assertEquals(20L, dto2.getCustomerId());
        assertEquals("musteri_iki", dto2.getCustomerUsername());
        assertEquals("Leyla Məmmədova", dto2.getCustomerFullName());

        // Sıralama və səhifələmə yoxlanışı (orderDate DESC, id DESC)
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAll(captor.capture());
        Pageable captured = captor.getValue();
        assertEquals(1, captured.getPageNumber());
        assertEquals(5, captured.getPageSize());

        Sort sort = captured.getSort();
        assertNotNull(sort);
        Sort.Order orderDateOrder = sort.getOrderFor("orderDate");
        Sort.Order idOrder = sort.getOrderFor("id");
        assertNotNull(orderDateOrder);
        assertEquals(Sort.Direction.DESC, orderDateOrder.getDirection());
        assertNotNull(idOrder);
        assertEquals(Sort.Direction.DESC, idOrder.getDirection());
    }

    @Test
    @DisplayName("getAllOrdersForAdmin: Pageable null olduqda susmaya görə page=0, size=10 istifadə etməlidir")
    void getAllOrdersForAdmin_whenPageableNull_usesDefaultPagination() {
        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

        Page<OrderResponseDto> result = orderService.getAllOrdersForAdmin(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAll(captor.capture());
        Pageable captured = captor.getValue();
        assertEquals(0, captured.getPageNumber());
        assertEquals(10, captured.getPageSize());
    }

    @Test
    @DisplayName("getOrderDetailsForAdmin: Yanlış ID (null, 0, mənfi) olduqda IllegalArgumentException atmalıdır")
    void getOrderDetailsForAdmin_withInvalidId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderDetailsForAdmin(null));
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderDetailsForAdmin(0L));
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderDetailsForAdmin(-5L));

        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("getOrderDetailsForAdmin: Sifariş tapılmadıqda EntityNotFoundException('Sifariş tapılmadı!') atmalıdır")
    void getOrderDetailsForAdmin_whenNotFound_throwsEntityNotFoundException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> orderService.getOrderDetailsForAdmin(999L));

        assertEquals("Sifariş tapılmadı!", ex.getMessage());
        verify(orderRepository).findById(999L);
    }

    @Test
    @DisplayName("getOrderDetailsForAdmin: Sifariş tapıldıqda tarixi qiymətlər qorunmalı, deaktiv məhsullar gizlənməməlidir")
    void getOrderDetailsForAdmin_preservesHistoricalPricesAndShowsInactiveProducts() {
        Order order = createSampleOrder(55L, "ORD-ADMIN-55", activeUser, OrderStatus.HAZIRLANIR, new BigDecimal("350.00"));
        order.setShippingAddress("Nizami ray., Q.Qarayev pr. 12");
        order.setTrackingNumber("TRK-882244");
        order.setOrderNotes("Təcili karqo");

        // Cari qiyməti dəyişmiş məhsul
        activeProduct.setPrice(new BigDecimal("999.00")); // cari qiymət artıb

        OrderItem item1 = new OrderItem();
        item1.setId(101L);
        item1.setOrder(order);
        item1.setProduct(activeProduct);
        item1.setQuantity(2);
        item1.setPriceAtPurchase(new BigDecimal("100.00")); // tarixi qiymət

        // Deaktiv məhsul
        inactiveProduct.setIsActive(false);

        OrderItem item2 = new OrderItem();
        item2.setId(102L);
        item2.setOrder(order);
        item2.setProduct(inactiveProduct);
        item2.setQuantity(1);
        item2.setPriceAtPurchase(new BigDecimal("150.00")); // tarixi qiymət

        order.setItems(List.of(item1, item2));

        when(orderRepository.findById(55L)).thenReturn(Optional.of(order));

        OrderResponseDto result = orderService.getOrderDetailsForAdmin(55L);

        assertNotNull(result);
        assertEquals(55L, result.getId());
        assertEquals("ORD-ADMIN-55", result.getOrderNumber());
        assertEquals("Nizami ray., Q.Qarayev pr. 12", result.getShippingAddress());
        assertEquals("TRK-882244", result.getTrackingNumber());
        assertEquals("Təcili karqo", result.getOrderNotes());
        assertEquals(3L, result.getTotalItems());
        assertEquals(new BigDecimal("350.00"), result.getTotalAmount());

        // Maddələrin yoxlanışı
        assertEquals(2, result.getItems().size());

        // Maddə 1: Tarixi qiymət (100.00), cari 999.00 deyil!
        OrderItemResponseDto itemDto1 = result.getItems().get(0);
        assertEquals(new BigDecimal("100.00"), itemDto1.getPriceAtPurchase());
        assertEquals(new BigDecimal("200.00"), itemDto1.getSubtotal());

        // Maddə 2: Deaktiv məhsul gizlənməyib
        OrderItemResponseDto itemDto2 = result.getItems().get(1);
        assertEquals(102L, itemDto2.getProductId());
        assertEquals("Köhnə model amortizator", itemDto2.getProductName());
        assertEquals(new BigDecimal("150.00"), itemDto2.getPriceAtPurchase());
        assertEquals(new BigDecimal("150.00"), itemDto2.getSubtotal());
    }

    @Test
    @DisplayName("updateOrderStatusForAdmin: Üç düzgün keçid uğurla icra edilməlidir")
    void updateOrderStatusForAdmin_validTransitions_success() {
        // 1. GÖZLƏYİR -> HAZIRLANIR
        Order order1 = createSampleOrder(10L, "ORD-1", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("100.00"));
        when(orderRepository.findByIdWithLock(10L)).thenReturn(Optional.of(order1));

        orderService.updateOrderStatusForAdmin(10L, OrderStatus.HAZIRLANIR);
        assertEquals(OrderStatus.HAZIRLANIR, order1.getStatus());
        verify(orderRepository).save(order1);

        // 2. HAZIRLANIR -> KARQOYA_VERİLDİ (String parametri ilə də yoxlanılır)
        Order order2 = createSampleOrder(20L, "ORD-2", activeUser, OrderStatus.HAZIRLANIR, new BigDecimal("200.00"));
        when(orderRepository.findByIdWithLock(20L)).thenReturn(Optional.of(order2));

        orderService.updateOrderStatusForAdmin(20L, "KARQOYA_VERİLDİ");
        assertEquals(OrderStatus.KARQOYA_VERİLDİ, order2.getStatus());
        verify(orderRepository).save(order2);

        // 3. KARQOYA_VERİLDİ -> ÇATDIRILDI
        Order order3 = createSampleOrder(30L, "ORD-3", activeUser, OrderStatus.KARQOYA_VERİLDİ, new BigDecimal("300.00"));
        when(orderRepository.findByIdWithLock(30L)).thenReturn(Optional.of(order3));

        orderService.updateOrderStatusForAdmin(30L, OrderStatus.ÇATDIRILDI);
        assertEquals(OrderStatus.ÇATDIRILDI, order3.getStatus());
        verify(orderRepository).save(order3);
    }

    @Test
    @DisplayName("updateOrderStatusForAdmin: Mərhələ atlamaq və geri keçid bloklanmalıdır")
    void updateOrderStatusForAdmin_skippedAndBackwardTransitions_blocked() {
        // Mərhələ atlama: GÖZLƏYİR -> KARQOYA_VERİLDİ
        Order order1 = createSampleOrder(11L, "ORD-SKIP-1", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("100.00"));
        when(orderRepository.findByIdWithLock(11L)).thenReturn(Optional.of(order1));
        assertThrows(IllegalStateException.class, () -> orderService.updateOrderStatusForAdmin(11L, OrderStatus.KARQOYA_VERİLDİ));

        // Mərhələ atlama: GÖZLƏYİR -> ÇATDIRILDI
        assertThrows(IllegalStateException.class, () -> orderService.updateOrderStatusForAdmin(11L, OrderStatus.ÇATDIRILDI));

        // Mərhələ atlama: HAZIRLANIR -> ÇATDIRILDI
        Order order2 = createSampleOrder(12L, "ORD-SKIP-2", activeUser, OrderStatus.HAZIRLANIR, new BigDecimal("100.00"));
        when(orderRepository.findByIdWithLock(12L)).thenReturn(Optional.of(order2));
        assertThrows(IllegalStateException.class, () -> orderService.updateOrderStatusForAdmin(12L, OrderStatus.ÇATDIRILDI));

        // Geri keçid: KARQOYA_VERİLDİ -> HAZIRLANIR
        Order order3 = createSampleOrder(13L, "ORD-BACK-1", activeUser, OrderStatus.KARQOYA_VERİLDİ, new BigDecimal("100.00"));
        when(orderRepository.findByIdWithLock(13L)).thenReturn(Optional.of(order3));
        assertThrows(IllegalStateException.class, () -> orderService.updateOrderStatusForAdmin(13L, OrderStatus.HAZIRLANIR));

        // Geri keçid: ÇATDIRILDI -> KARQOYA_VERİLDİ
        Order order4 = createSampleOrder(14L, "ORD-BACK-2", activeUser, OrderStatus.ÇATDIRILDI, new BigDecimal("100.00"));
        when(orderRepository.findByIdWithLock(14L)).thenReturn(Optional.of(order4));
        assertThrows(IllegalStateException.class, () -> orderService.updateOrderStatusForAdmin(14L, OrderStatus.KARQOYA_VERİLDİ));
    }

    @Test
    @DisplayName("updateOrderStatusForAdmin: Bu marşrutdan ləğv etmə və ya 'Gözləyir' təyini qadağandır")
    void updateOrderStatusForAdmin_cancelOrPendingTarget_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatusForAdmin(15L, OrderStatus.LƏĞV_EDİLDİ));
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatusForAdmin(15L, "LƏĞV_EDİLDİ"));
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatusForAdmin(15L, OrderStatus.GÖZLƏYİR));
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatusForAdmin(15L, "GÖZLƏYİR"));
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("updateOrderStatusForAdmin: Təkrar təsdiq idempotentdir, statusu növbəti mərhələyə keçirmir")
    void updateOrderStatusForAdmin_repeatedRequest_idempotentNoFurtherAdvance() {
        Order order = createSampleOrder(16L, "ORD-REPEAT", activeUser, OrderStatus.HAZIRLANIR, new BigDecimal("100.00"));
        when(orderRepository.findByIdWithLock(16L)).thenReturn(Optional.of(order));

        // Cari status artıq HAZIRLANIR-dır və təkrar HAZIRLANIR göndərilir
        orderService.updateOrderStatusForAdmin(16L, OrderStatus.HAZIRLANIR);

        // Status dəyişməz qalır (KARQOYA_VERİLDİ olmur)
        assertEquals(OrderStatus.HAZIRLANIR, order.getStatus());
        verify(orderRepository, never()).save(order);
    }

    @Test
    @DisplayName("updateOrderStatusForAdmin: Ləğv edilmiş sifarişə təsdiq cəhdi rədd edilməlidir")
    void updateOrderStatusForAdmin_cancelledOrder_throwsIllegalStateException() {
        Order order = createSampleOrder(17L, "ORD-CANCELLED", activeUser, OrderStatus.LƏĞV_EDİLDİ, new BigDecimal("100.00"));
        when(orderRepository.findByIdWithLock(17L)).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.updateOrderStatusForAdmin(17L, OrderStatus.HAZIRLANIR));

        assertTrue(ex.getMessage().contains("Ləğv edilmiş"));
        verify(orderRepository, never()).save(order);
    }

    @Test
    @DisplayName("updateOrderStatusForAdmin: Stok, məhsul sayı, ödəniş statusu və qiymətlər dəyişməməlidir")
    void updateOrderStatusForAdmin_preservesStockPaymentAndAmounts() {
        Order order = createSampleOrder(18L, "ORD-INTEGRITY", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("150.00"));
        order.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);

        activeProduct.setStockQuantity(25);
        OrderItem item = new OrderItem();
        item.setId(501L);
        item.setOrder(order);
        item.setProduct(activeProduct);
        item.setQuantity(3);
        item.setPriceAtPurchase(new BigDecimal("50.00"));
        order.getItems().add(item);

        when(orderRepository.findByIdWithLock(18L)).thenReturn(Optional.of(order));

        orderService.updateOrderStatusForAdmin(18L, OrderStatus.HAZIRLANIR);

        assertEquals(OrderStatus.HAZIRLANIR, order.getStatus());
        assertEquals(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR, order.getPaymentStatus());
        assertEquals(new BigDecimal("150.00"), order.getTotalAmount());
        assertEquals(25, activeProduct.getStockQuantity());
        verifyNoInteractions(productRepository);
        verifyNoInteractions(cartRepository);
    }

    @Test
    @DisplayName("updateOrderStatusForAdmin: Sifariş ID tapılmadıqda və ya mənfi olduqda müvafiq xətalar atılır")
    void updateOrderStatusForAdmin_notFoundOrInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatusForAdmin(null, OrderStatus.HAZIRLANIR));
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatusForAdmin(0L, OrderStatus.HAZIRLANIR));
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatusForAdmin(-2L, OrderStatus.HAZIRLANIR));

        when(orderRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> orderService.updateOrderStatusForAdmin(999L, OrderStatus.HAZIRLANIR));
    }

    @Test
    @DisplayName("updateOrderStatusForAdmin: Hədəf status boş və ya naməlum olduqda IllegalArgumentException atılır")
    void updateOrderStatusForAdmin_emptyOrUnknownStatus_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatusForAdmin(1L, (String) null));
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatusForAdmin(1L, "   "));
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatusForAdmin(1L, "INVALID_STATUS"));
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatusForAdmin(1L, (OrderStatus) null));
    }

    private Order createSampleOrder(Long id, String orderNumber, User user, OrderStatus status, BigDecimal totalAmount) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(status);
        order.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);
        order.setPaymentMethod(PaymentMethod.NAĞD);
        order.setTotalAmount(totalAmount);
        order.setShippingAddress("Nərimanov ray., Təbriz küç. 45");
        order.setTrackingNumber("TRK-778899");
        order.setOrderNotes("Təcili çatdırılma xahiş olunur");
        order.setItems(new ArrayList<>());
        return order;
    }

    @Test
    @DisplayName("Sifariş yaradılarkən refresh xətası baş verdikdə sifariş saxlanmır, stok və səbət dəyişmir")
    void createOrderFromCart_WhenRefreshFails_ShouldThrowExceptionAndNotModifyStockOrCartOrSaveOrder() {
        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(activeUser);
        CartItem item = new CartItem();
        item.setId(20L);
        item.setCart(cart);
        item.setProduct(activeProduct);
        item.setQuantity(2);
        cart.getItems().add(item);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(activeProduct));

        doAnswer(invocation -> {
            Object entity = invocation.getArgument(0);
            if (entity instanceof Product) {
                throw new jakarta.persistence.PersistenceException("DB refresh error on product");
            }
            return null;
        }).when(entityManager).refresh(any());

        assertThrows(jakarta.persistence.PersistenceException.class, () ->
                orderService.createOrderFromCart(1L, "Bakı, Nizami küç. 10", "Qeyd")
        );

        verify(orderRepository, never()).save(any());
        assertEquals(10, activeProduct.getStockQuantity());
        verify(productRepository, never()).save(any());
        assertEquals(1, cart.getItems().size());
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sifariş ləğv edilərkən refresh xətası baş verdikdə sifariş statusu və məhsul stoku dəyişmir")
    void cancelOrder_WhenRefreshFails_ShouldThrowExceptionAndNotModifyStockOrOrderStatus() {
        Order order = createSampleOrder(100L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("31.00"));
        OrderItem item = new OrderItem();
        item.setId(50L);
        item.setOrder(order);
        item.setProduct(activeProduct);
        item.setQuantity(2);
        order.getItems().add(item);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeUser));
        when(orderRepository.findByIdAndUserIdWithLock(100L, 1L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithLock(101L)).thenReturn(Optional.of(activeProduct));

        doAnswer(invocation -> {
            Object entity = invocation.getArgument(0);
            if (entity instanceof Product) {
                throw new jakarta.persistence.PersistenceException("DB refresh error on product");
            }
            return null;
        }).when(entityManager).refresh(any());

        assertThrows(jakarta.persistence.PersistenceException.class, () ->
                orderService.cancelOrder(1L, 100L)
        );

        assertEquals(OrderStatus.GÖZLƏYİR, order.getStatus());
        verify(orderRepository, never()).save(any());
        assertEquals(10, activeProduct.getStockQuantity());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin status yeniləyərkən refresh xətası baş verdikdə status saxlanmır və dəyişmir")
    void updateOrderStatusForAdmin_WhenRefreshFails_ShouldThrowExceptionAndNotUpdateStatus() {
        Order order = createSampleOrder(100L, "ORD-12345", activeUser, OrderStatus.GÖZLƏYİR, new BigDecimal("31.00"));

        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(order));
        doThrow(new jakarta.persistence.PersistenceException("DB refresh error on order"))
                .when(entityManager).refresh(order);

        assertThrows(jakarta.persistence.PersistenceException.class, () ->
                orderService.updateOrderStatusForAdmin(100L, "HAZIRLANIR")
        );

        assertEquals(OrderStatus.GÖZLƏYİR, order.getStatus());
        verify(orderRepository, never()).save(any());
    }
}
