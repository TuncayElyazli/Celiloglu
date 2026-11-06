package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.dto.response.OrderItemResponseDto;
import com.celiloglu.backend.dto.response.OrderResponseDto;
import com.celiloglu.backend.entity.Cart;
import com.celiloglu.backend.entity.CartItem;
import com.celiloglu.backend.entity.Order;
import com.celiloglu.backend.entity.OrderItem;
import com.celiloglu.backend.entity.Product;
import com.celiloglu.backend.entity.User;
import com.celiloglu.backend.enums.OrderStatus;
import com.celiloglu.backend.enums.PaymentMethod;
import com.celiloglu.backend.enums.PaymentStatus;
import com.celiloglu.backend.repository.CartRepository;
import com.celiloglu.backend.repository.OrderRepository;
import com.celiloglu.backend.repository.ProductRepository;
import com.celiloglu.backend.repository.UserRepository;
import com.celiloglu.backend.service.OrderService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final EntityManager entityManager;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository,
                            CartRepository cartRepository,
                            EntityManager entityManager) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager boş ola bilməz!");
    }

    @Override
    public Page<OrderResponseDto> getUserOrders(Long userId, Pageable pageable) {
        validateActiveUser(userId);

        Sort requiredSort = Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("id"));
        Pageable effectivePageable;
        if (pageable == null) {
            effectivePageable = PageRequest.of(0, 10, requiredSort);
        } else {
            int pageNumber = pageable.getPageNumber();
            int pageSize = pageable.getPageSize() > 0 ? pageable.getPageSize() : 10;
            effectivePageable = PageRequest.of(pageNumber, pageSize, requiredSort);
        }

        return orderRepository.findByUserId(userId, effectivePageable)
                .map(this::mapToOrderResponseDto);
    }

    @Override
    public OrderResponseDto getOrderDetails(Long userId, Long orderId) {
        validateActiveUser(userId);

        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Sifariş ID-si müsbət ədəd olmalıdır!");
        }

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Sifariş tapılmadı!"));

        return mapToOrderResponseDto(order);
    }

    @Override
    @Transactional
    public OrderResponseDto createOrderFromCart(Long userId, String shippingAddress, String orderNotes) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("İstifadəçi ID-si müsbət ədəd olmalıdır!");
        }

        // 1. Ünvan və qeydi trim et, boş olduqda null saxla, sütun limitini (maksimum 255 simvol) yoxla
        String cleanShippingAddress = (shippingAddress != null && !shippingAddress.trim().isEmpty())
                ? shippingAddress.trim() : null;
        String cleanOrderNotes = (orderNotes != null && !orderNotes.trim().isEmpty())
                ? orderNotes.trim() : null;

        if (cleanShippingAddress != null && cleanShippingAddress.length() > 255) {
            throw new IllegalArgumentException("Çatdırılma ünvanı maksimum 255 simvol ola bilər!");
        }
        if (cleanOrderNotes != null && cleanOrderNotes.length() > 255) {
            throw new IllegalArgumentException("Sifariş qeydi maksimum 255 simvol ola bilər!");
        }

        // 2. İstifadəçini PESSIMISTIC_WRITE ilə kilidlə, mövcudluğunu və aktivliyini yoxla
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new EntityNotFoundException("İstifadəçi tapılmadı! ID: " + userId));

        entityManager.refresh(user);

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("İstifadəçi hesabı deaktivdir!");
        }

        // 3. Səbəti istifadəçi kilidindən sonra oxu; olmayan və boş səbəti rədd et
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null && user.getCart() != null) {
            cart = user.getCart();
        }

        if (cart != null) {
            entityManager.refresh(cart);
        }

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Səbət boşdur!");
        }

        // 4. Səbət maddələrini yoxla və eyni məhsul bir neçə sətirdədirsə miqdarları overflow-suz cəmlə
        Map<Long, Long> aggregatedQuantities = new LinkedHashMap<>();
        List<CartItem> validCartItems = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            if (item == null) {
                continue;
            }
            if (item.getProduct() == null || item.getProduct().getId() == null) {
                throw new IllegalStateException("Səbətdəki məhsul məlumatı tam deyil!");
            }

            Long productId = item.getProduct().getId();
            if (productId <= 0) {
                throw new IllegalArgumentException("Məhsul ID-si müsbət ədəd olmalıdır!");
            }

            Integer qty = item.getQuantity();
            if (qty == null || qty < 1) {
                throw new IllegalArgumentException("Məhsul sayı ən azı 1 olmalıdır!");
            }

            long currentSum = aggregatedQuantities.getOrDefault(productId, 0L);
            long newSum = currentSum + qty;
            if (newSum > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Məhsul sayı icazə verilən maksimum həddi aşır!");
            }
            aggregatedQuantities.put(productId, newSum);
            validCartItems.add(item);
        }

        if (validCartItems.isEmpty() || aggregatedQuantities.isEmpty()) {
            throw new IllegalStateException("Səbət boşdur!");
        }

        // 5. Deadlock-un qarşısını almaq üçün məhsul kilidlərini həmişə productId artan sıra ilə al
        List<Long> sortedProductIds = aggregatedQuantities.keySet().stream().sorted().toList();
        Map<Long, Product> lockedProducts = new LinkedHashMap<>();
        for (Long pid : sortedProductIds) {
            Product lockedProduct = productRepository.findByIdWithLock(pid)
                    .orElseThrow(() -> new EntityNotFoundException("Məhsul tapılmadı! ID: " + pid));

            entityManager.refresh(lockedProduct);

            lockedProducts.put(pid, lockedProduct);
        }

        // 6. Dəyişikliklərdən əvvəl bütün yoxlamaları tamamla (aktivlik, qiymət, stok)
        for (Map.Entry<Long, Long> entry : aggregatedQuantities.entrySet()) {
            Long pid = entry.getKey();
            long totalQty = entry.getValue();
            Product p = lockedProducts.get(pid);

            if (!Boolean.TRUE.equals(p.getIsActive())) {
                throw new IllegalStateException("Məhsul deaktivdir və sifariş edilə bilməz: " + p.getName());
            }

            BigDecimal price = p.getPrice();
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Məhsulun qiyməti düzgün deyil: " + p.getName());
            }

            Integer stock = p.getStockQuantity();
            if (stock == null || stock <= 0) {
                throw new IllegalStateException("Məhsul stokda yoxdur: " + p.getName());
            }

            if (totalQty > stock) {
                throw new IllegalArgumentException("Tələb olunan miqdar (" + totalQty + ") mövcud stokdan (" + stock + ") çox ola bilməz: " + p.getName());
            }
        }

        // 7. Stoku sifariş miqdarı qədər azalt (sıfır ola bilər, mənfi ola bilməz)
        for (Map.Entry<Long, Long> entry : aggregatedQuantities.entrySet()) {
            Long pid = entry.getKey();
            long totalQty = entry.getValue();
            Product p = lockedProducts.get(pid);
            int newStock = (int) (p.getStockQuantity() - totalQty);
            if (newStock < 0) {
                throw new IllegalStateException("Məhsul stoku mənfi ola bilməz: " + p.getName());
            }
            p.setStockQuantity(newStock);
            productRepository.save(p);
        }

        // 8. Order və OrderItem əlaqələrini qur, qiymət snapshot-u götür və məbləğləri BigDecimal ilə hesabla
        Order order = new Order();
        String orderNumber;
        do {
            orderNumber = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (orderRepository.findByOrderNumber(orderNumber).isPresent());

        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.GÖZLƏYİR);
        order.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);
        order.setPaymentMethod(PaymentMethod.NAĞD);
        order.setShippingAddress(cleanShippingAddress);
        order.setOrderNotes(cleanOrderNotes);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : validCartItems) {
            Product lockedProduct = lockedProducts.get(cartItem.getProduct().getId());
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(lockedProduct);
            orderItem.setQuantity(cartItem.getQuantity());

            BigDecimal serverPrice = lockedProduct.getPrice();
            orderItem.setPriceAtPurchase(serverPrice);

            BigDecimal lineSubtotal = serverPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineSubtotal);

            orderItems.add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        // 9. Sifarişi saxla
        Order savedOrder = orderRepository.save(order);

        // 10. Eyni transaction daxilində yalnız cari istifadəçinin səbət maddələrini təmizlə
        for (CartItem item : cart.getItems()) {
            if (item != null) {
                item.setCart(null);
            }
        }
        cart.getItems().clear();
        cartRepository.save(cart);

        // 11. Cavab DTO-sunu qaytar
        return mapToOrderResponseDto(savedOrder);
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("İstifadəçi ID-si müsbət ədəd olmalıdır!");
        }
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Sifariş ID-si müsbət ədəd olmalıdır!");
        }

        // 1. İstifadəçini PESSIMISTIC_WRITE ilə kilidlə, mövcudluğunu və aktivliyini yoxla
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new EntityNotFoundException("İstifadəçi tapılmadı! ID: " + userId));

        entityManager.refresh(user);

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("İstifadəçi hesabı deaktivdir!");
        }

        // 2. İstifadəçiyə aid sifarişi PESSIMISTIC_WRITE ilə kilidlə və təzə vəziyyətini oxu
        Order order = orderRepository.findByIdAndUserIdWithLock(orderId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Sifariş tapılmadı!"));

        if (order.getUser() == null || !userId.equals(order.getUser().getId())) {
            throw new EntityNotFoundException("Sifariş tapılmadı!");
        }

        entityManager.refresh(order);

        // 3. İdempotentlik: Sifariş artıq ləğv edilibsə heç bir dəyişiklik etmədən sakitcə tamamla
        if (order.getStatus() == OrderStatus.LƏĞV_EDİLDİ) {
            return;
        }

        // 4. Status yoxlamaları: yalnız GÖZLƏYİR və ÖDƏNİŞ_GÖZLƏYİR ləğv edilə bilər
        if (order.getStatus() != OrderStatus.GÖZLƏYİR) {
            throw new IllegalStateException("Yalnız 'Gözləyir' statusunda olan sifarişlər ləğv edilə bilər! Cari status: " + order.getStatus());
        }

        if (order.getPaymentStatus() != PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR) {
            throw new IllegalStateException("Ödənişi başlanmış və ya tamamlanmış sifarişlər ləğv edilə bilməz! Cari ödəniş statusu: " + order.getPaymentStatus());
        }

        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            throw new IllegalStateException("Ödəniş qeydi olan sifariş bu bölmədən ləğv edilə bilməz!");
        }

        // 5. Məhsul miqdarlarını yoxla və eyni məhsul bir neçə sətirdədirsə overflow-suz cəmlə
        Map<Long, Long> aggregatedReturnQuantities = new LinkedHashMap<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item == null) {
                    continue;
                }
                if (item.getProduct() == null || item.getProduct().getId() == null) {
                    throw new IllegalStateException("Sifarişdəki məhsul məlumatı tam deyil!");
                }

                Long productId = item.getProduct().getId();
                if (productId <= 0) {
                    throw new IllegalArgumentException("Məhsul ID-si müsbət ədəd olmalıdır!");
                }

                Integer qty = item.getQuantity();
                if (qty == null || qty < 1) {
                    throw new IllegalArgumentException("Məhsul sayı ən azı 1 olmalıdır!");
                }

                long currentSum = aggregatedReturnQuantities.getOrDefault(productId, 0L);
                long newSum = currentSum + qty;
                if (newSum > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Qaytarılan məhsul sayı icazə verilən maksimum həddi aşır!");
                }
                aggregatedReturnQuantities.put(productId, newSum);
            }
        }

        // 6. Deadlock-un qarşısını almaq üçün məhsul kilidlərini artan productId sırası ilə al
        List<Long> sortedProductIds = aggregatedReturnQuantities.keySet().stream().sorted().toList();
        Map<Long, Product> lockedProducts = new LinkedHashMap<>();
        for (Long pid : sortedProductIds) {
            Product lockedProduct = productRepository.findByIdWithLock(pid)
                    .orElseThrow(() -> new EntityNotFoundException("Məhsul tapılmadı! ID: " + pid));

            entityManager.refresh(lockedProduct);

            lockedProducts.put(pid, lockedProduct);
        }

        // 7. Dəyişikliklərdən əvvəl bütün stok overflow yoxlamalarını tamamla
        for (Map.Entry<Long, Long> entry : aggregatedReturnQuantities.entrySet()) {
            Long pid = entry.getKey();
            long returnQty = entry.getValue();
            Product p = lockedProducts.get(pid);

            int currentStock = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
            if (currentStock < 0) {
                throw new IllegalStateException("Mövcud məhsul stoku mənfi ola bilməz: " + p.getName());
            }

            long newStockLong = (long) currentStock + returnQty;
            if (newStockLong > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Stok miqdarı maksimum həddi aşır: " + p.getName());
            }
        }

        // 8. Stoku artır (qeyri-aktiv məhsulların da stoku bərpa olunur, isActive toxunulmur)
        for (Map.Entry<Long, Long> entry : aggregatedReturnQuantities.entrySet()) {
            Long pid = entry.getKey();
            long returnQty = entry.getValue();
            Product p = lockedProducts.get(pid);

            int currentStock = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
            p.setStockQuantity((int) (currentStock + returnQty));
            productRepository.save(p);
        }

        // 9. Sifariş statusunu LƏĞV_EDİLDİ et və yadda saxla
        order.setStatus(OrderStatus.LƏĞV_EDİLDİ);
        orderRepository.save(order);
    }

    private User validateActiveUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("İstifadəçi ID-si müsbət ədəd olmalıdır!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("İstifadəçi tapılmadı! ID: " + userId));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("İstifadəçi hesabı deaktivdir!");
        }

        return user;
    }

    private OrderResponseDto mapToOrderResponseDto(Order order) {
        if (order == null) {
            return null;
        }

        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setOrderNotes(order.getOrderNotes());

        List<OrderItemResponseDto> itemDtos = new ArrayList<>();
        long totalItems = 0L;
        BigDecimal calculatedTotal = BigDecimal.ZERO;

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item == null) {
                    continue;
                }
                OrderItemResponseDto itemDto = mapToOrderItemResponseDto(item);
                itemDtos.add(itemDto);

                if (itemDto.getQuantity() != null && itemDto.getQuantity() > 0) {
                    totalItems += itemDto.getQuantity();
                }
                if (itemDto.getSubtotal() != null) {
                    calculatedTotal = calculatedTotal.add(itemDto.getSubtotal());
                }
            }
        }

        dto.setItems(itemDtos);
        dto.setTotalItems(totalItems);
        dto.setTotalAmount(order.getTotalAmount() != null ? order.getTotalAmount() : calculatedTotal);

        if (order.getUser() != null) {
            User user = order.getUser();
            dto.setCustomerId(user.getId());
            dto.setCustomerUsername(user.getUsername());
            String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
            String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
            String fullName = (firstName + " " + lastName).trim();
            dto.setCustomerFullName(!fullName.isEmpty() ? fullName : user.getUsername());
            dto.setCustomerEmail(user.getEmail());
            dto.setCustomerPhone(user.getPhone());
            dto.setCustomerCompanyName(user.getCompanyName());
            dto.setCustomerTin(user.getTin());
        }

        boolean canCancel = order.getStatus() == OrderStatus.GÖZLƏYİR
                && order.getPaymentStatus() == PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR
                && (order.getPayments() == null || order.getPayments().isEmpty());
        dto.setCanCancel(canCancel);

        return dto;
    }

    private OrderItemResponseDto mapToOrderItemResponseDto(OrderItem item) {
        OrderItemResponseDto dto = new OrderItemResponseDto();
        dto.setId(item.getId());

        Product product = item.getProduct();
        if (product != null) {
            dto.setProductId(product.getId());
            dto.setProductName(product.getName());
            dto.setProductCode(product.getCode());
            dto.setImageUrl(product.getImageUrl());
            dto.setCurrentStock(product.getStockQuantity());
        }

        int quantity = (item.getQuantity() != null && item.getQuantity() > 0) ? item.getQuantity() : 0;
        dto.setQuantity(quantity);

        BigDecimal priceAtPurchase = item.getPriceAtPurchase() != null ? item.getPriceAtPurchase() : BigDecimal.ZERO;
        dto.setPriceAtPurchase(priceAtPurchase);

        BigDecimal subtotal = priceAtPurchase.multiply(BigDecimal.valueOf(quantity));
        dto.setSubtotal(subtotal);

        return dto;
    }

    @Override
    public Page<OrderResponseDto> getAllOrdersForAdmin(Pageable pageable) {
        Sort requiredSort = Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("id"));
        Pageable effectivePageable;
        if (pageable == null) {
            effectivePageable = PageRequest.of(0, 10, requiredSort);
        } else {
            int pageNumber = Math.max(pageable.getPageNumber(), 0);
            int pageSize = pageable.getPageSize() > 0 ? pageable.getPageSize() : 10;
            effectivePageable = PageRequest.of(pageNumber, pageSize, requiredSort);
        }

        return orderRepository.findAll(effectivePageable)
                .map(this::mapToOrderResponseDto);
    }

    @Override
    public OrderResponseDto getOrderDetailsForAdmin(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Sifariş ID-si müsbət ədəd olmalıdır!");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Sifariş tapılmadı!"));

        return mapToOrderResponseDto(order);
    }

    @Override
    @Transactional
    public void updateOrderStatusForAdmin(Long orderId, String targetStatus) {
        if (targetStatus == null || targetStatus.isBlank()) {
            throw new IllegalArgumentException("Hədəf status qeyd edilməlidir!");
        }
        OrderStatus desiredStatus;
        try {
            desiredStatus = OrderStatus.valueOf(targetStatus.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Naməlum status: " + targetStatus);
        }
        updateOrderStatusInternal(orderId, desiredStatus);
    }

    @Override
    @Transactional
    public void updateOrderStatusForAdmin(Long orderId, OrderStatus targetStatus) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("Hədəf status qeyd edilməlidir!");
        }
        updateOrderStatusInternal(orderId, targetStatus);
    }

    private void updateOrderStatusInternal(Long orderId, OrderStatus desiredStatus) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Sifariş ID-si müsbət ədəd olmalıdır!");
        }


        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isWarehouse = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_WAREHOUSE_MANAGER".equals(a.getAuthority()));

        if (isWarehouse && desiredStatus != OrderStatus.KARQOYA_VERİLDİ) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Anbar müdiri yalnız 'Karqoya verildi' status keçidini təsdiq edə bilər!"
            );
        }
        if (desiredStatus == OrderStatus.LƏĞV_EDİLDİ) {
            throw new IllegalArgumentException("Bu bölmədən sifariş ləğv edilə bilməz!");
        }
        if (desiredStatus == OrderStatus.GÖZLƏYİR) {
            throw new IllegalArgumentException("Sifariş statusu 'Gözləyir' vəziyyətinə geri qaytarıla bilməz!");
        }
        if (desiredStatus != OrderStatus.HAZIRLANIR
                && desiredStatus != OrderStatus.KARQOYA_VERİLDİ
                && desiredStatus != OrderStatus.ÇATDIRILDI) {
            throw new IllegalArgumentException("İcazəsiz hədəf status: " + desiredStatus);
        }

        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Sifariş tapılmadı!"));

        entityManager.refresh(order);

        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == desiredStatus) {
            return;
        }

        if (currentStatus == OrderStatus.LƏĞV_EDİLDİ) {
            throw new IllegalStateException("Ləğv edilmiş sifarişin statusu dəyişdirilə bilməz!");
        }
        if (currentStatus == OrderStatus.ÇATDIRILDI) {
            throw new IllegalStateException("Çatdırılmış sifarişin statusu dəyişdirilə bilməz!");
        }

        boolean isValidTransition = false;
        if (currentStatus == OrderStatus.GÖZLƏYİR && desiredStatus == OrderStatus.HAZIRLANIR) {
            isValidTransition = true;
        } else if (currentStatus == OrderStatus.HAZIRLANIR && desiredStatus == OrderStatus.KARQOYA_VERİLDİ) {
            isValidTransition = true;
        } else if (currentStatus == OrderStatus.KARQOYA_VERİLDİ && desiredStatus == OrderStatus.ÇATDIRILDI) {
            isValidTransition = true;
        }

        if (!isValidTransition) {
            throw new IllegalStateException(String.format(
                    "Status keçidi yolverilməzdir! Cari status: '%s', cəhd edilən hədəf: '%s'. Keçidlər yalnız ardıcıl olmalıdır: Gözləyir -> Hazırlanır -> Karqoya verildi -> Çatdırıldı.",
                    currentStatus != null ? currentStatus.getDisplayName() : "Təyin edilməyib",
                    desiredStatus.getDisplayName()
            ));
        }

        order.setStatus(desiredStatus);
        orderRepository.save(order);
    }
}
