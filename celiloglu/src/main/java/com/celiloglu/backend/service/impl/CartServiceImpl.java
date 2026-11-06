package com.celiloglu.backend.service.impl;

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
import com.celiloglu.backend.service.CartService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    public CartServiceImpl(CartRepository cartRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            EntityManager entityManager) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager boş ola bilməz!");
    }

    @Override
    public Long getCartTotalItems(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("İstifadəçi ID-si müsbət ədəd olmalıdır!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("İstifadəçi tapılmadı! ID: " + userId));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("İstifadəçi hesabı deaktivdir!");
        }

        Long total = cartRepository.sumQuantityByUserId(userId);
        return total != null ? total : 0L;
    }

    @Override
    public CartResponseDto getCart(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("İstifadəçi ID-si müsbət ədəd olmalıdır!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("İstifadəçi tapılmadı! ID: " + userId));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("İstifadəçi hesabı deaktivdir!");
        }

        Optional<Cart> cartOptional = cartRepository.findByUserId(userId);
        if (cartOptional.isEmpty()) {
            return new CartResponseDto(new ArrayList<>(), 0L, BigDecimal.ZERO);
        }

        return buildCartResponseDto(cartOptional.get());
    }

    @Override
    @Transactional
    public CartResponseDto addItemToCart(Long userId, CartItemRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Sorğu boş ola bilməz!");
        }

        Long productId = request.getProductId();
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Məhsul ID-si müsbət ədəd olmalıdır!");
        }

        Integer quantity = request.getQuantity();
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Məhsul sayı ən azı 1 olmalıdır!");
        }

        User user = getLockedActiveUser(userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Məhsul tapılmadı! ID: " + productId));

        return addProductToUserCart(user, product, quantity);
    }

    @Override
    @Transactional
    public CartResponseDto addItemByCode(Long userId, String code, Integer quantity) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Məhsul kodu boş ola bilməz!");
        }

        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Məhsul sayı ən azı 1 olmalıdır!");
        }

        User user = getLockedActiveUser(userId);

        String trimmedCode = code.trim();
        List<Product> matchingProducts = productRepository.findByCodeIgnoreCase(trimmedCode);
        if (matchingProducts.isEmpty()) {
            throw new EntityNotFoundException("Məhsul tapılmadı! Kod: " + trimmedCode);
        }
        if (matchingProducts.size() > 1) {
            throw new IllegalStateException("Hərf fərqinə görə bu koda uyğun bir neçə məhsul tapıldı (" + trimmedCode
                    + ")! Zəhmət olmasa dəqiq kodu daxil edin.");
        }
        Product product = matchingProducts.get(0);

        return addProductToUserCart(user, product, quantity);
    }

    @Override
    @Transactional
    public CartResponseDto updateItemQuantity(Long userId, Long productId, Integer quantity) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Məhsul ID-si müsbət ədəd olmalıdır!");
        }

        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Məhsul sayı ən azı 1 olmalıdır!");
        }

        User user = getLockedActiveUser(userId);

        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new EntityNotFoundException("Məhsul səbətdə tapılmadı! ID: " + productId);
        }

        CartItem targetItem = null;
        for (CartItem item : cart.getItems()) {
            if (item != null && item.getProduct() != null && productId.equals(item.getProduct().getId())) {
                targetItem = item;
                break;
            }
        }

        if (targetItem == null) {
            throw new EntityNotFoundException("Məhsul səbətdə tapılmadı! ID: " + productId);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Məhsul tapılmadı! ID: " + productId));

        entityManager.refresh(product);

        validateProductForCart(product);

        int stock = product.getStockQuantity();
        if (quantity > stock) {
            throw new IllegalArgumentException(
                    "Tələb olunan miqdar (" + quantity + ") mövcud stokdan (" + stock + ") çox ola bilməz!");
        }

        targetItem.setProduct(product);
        targetItem.setQuantity(quantity);

        Cart savedCart = cartRepository.save(cart);
        return buildCartResponseDto(savedCart);
    }

    @Override
    @Transactional
    public CartResponseDto removeItemFromCart(Long userId, Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Məhsul ID-si müsbət ədəd olmalıdır!");
        }

        User user = getLockedActiveUser(userId);

        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new EntityNotFoundException("Məhsul səbətdə tapılmadı! ID: " + productId);
        }

        CartItem targetItem = null;
        for (CartItem item : cart.getItems()) {
            if (item != null && item.getProduct() != null && productId.equals(item.getProduct().getId())) {
                targetItem = item;
                break;
            }
        }

        if (targetItem == null) {
            throw new EntityNotFoundException("Məhsul səbətdə tapılmadı! ID: " + productId);
        }

        cart.getItems().remove(targetItem);
        targetItem.setCart(null);

        Cart savedCart = cartRepository.save(cart);
        return buildCartResponseDto(savedCart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        User user = getLockedActiveUser(userId);

        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            return;
        }

        if (cart.getItems() != null && !cart.getItems().isEmpty()) {
            for (CartItem item : cart.getItems()) {
                if (item != null) {
                    item.setCart(null);
                }
            }
            cart.getItems().clear();
            cartRepository.save(cart);
        }
    }

    private User getLockedActiveUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("İstifadəçi ID-si müsbət ədəd olmalıdır!");
        }

        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new EntityNotFoundException("İstifadəçi tapılmadı! ID: " + userId));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("İstifadəçi hesabı deaktivdir!");
        }

        return user;
    }

    private void validateProductForCart(Product product) {
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new IllegalStateException("Məhsul deaktivdir və səbət əməliyyatlarına icazə verilmir!");
        }

        Integer stock = product.getStockQuantity();
        if (stock == null || stock <= 0) {
            throw new IllegalStateException("Məhsul stokda yoxdur!");
        }

        BigDecimal price = product.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Məhsulun qiyməti düzgün deyil!");
        }
    }

    private CartResponseDto addProductToUserCart(User user, Product product, Integer quantity) {
        entityManager.refresh(product);

        validateProductForCart(product);

        Cart cart = cartRepository.findByUserId(user.getId()).orElse(null);
        if (cart == null && user.getCart() != null) {
            cart = user.getCart();
        }

        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            user.setCart(cart);
            cart.setItems(new ArrayList<>());
        } else if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        CartItem existingItem = null;
        for (CartItem item : cart.getItems()) {
            if (item != null && item.getProduct() != null && product.getId().equals(item.getProduct().getId())) {
                existingItem = item;
                break;
            }
        }

        int stock = product.getStockQuantity();

        if (existingItem != null) {
            int currentQty = existingItem.getQuantity() != null ? existingItem.getQuantity() : 0;
            long newTotalLong = (long) currentQty + quantity;
            if (newTotalLong > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Məhsul sayı icazə verilən maksimum həddi aşır!");
            }
            int newTotal = (int) newTotalLong;
            if (newTotal > stock) {
                throw new IllegalArgumentException("Tələb olunan miqdar (" + newTotal + ") mövcud stokdan (" + stock + ") çox ola bilməz!");
            }
            existingItem.setQuantity(newTotal);
            existingItem.setProduct(product);
        } else {
            if (quantity > stock) {
                throw new IllegalArgumentException("Tələb olunan miqdar (" + quantity + ") mövcud stokdan (" + stock + ") çox ola bilməz!");
            }
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return buildCartResponseDto(savedCart);
    }

    private CartResponseDto buildCartResponseDto(Cart cart) {
        if (cart == null) {
            return new CartResponseDto(new ArrayList<>(), 0L, BigDecimal.ZERO);
        }

        List<CartItem> items = cart.getItems();
        if (items == null || items.isEmpty()) {
            return new CartResponseDto(new ArrayList<>(), 0L, BigDecimal.ZERO);
        }

        List<CartItemResponseDto> itemDtos = new ArrayList<>();
        long totalItems = 0L;
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem item : items) {
            if (item == null) {
                continue;
            }
            CartItemResponseDto itemDto = mapToCartItemResponseDto(item);
            itemDtos.add(itemDto);

            if (item.getQuantity() != null && item.getQuantity() > 0) {
                totalItems += item.getQuantity();
            }
            if (itemDto.getTotalPrice() != null) {
                totalPrice = totalPrice.add(itemDto.getTotalPrice());
            }
        }

        return new CartResponseDto(itemDtos, totalItems, totalPrice);
    }

    private CartItemResponseDto mapToCartItemResponseDto(CartItem item) {
        CartItemResponseDto dto = new CartItemResponseDto();
        dto.setId(item.getId());

        int quantity = (item.getQuantity() != null && item.getQuantity() > 0) ? item.getQuantity() : 0;
        dto.setQuantity(quantity);

        Product product = item.getProduct();
        if (product == null) {
            dto.setUnitPrice(BigDecimal.ZERO);
            dto.setTotalPrice(BigDecimal.ZERO);
            dto.setAvailable(false);
            dto.setAvailabilityMessage("Məhsul tapılmadı.");
            return dto;
        }

        dto.setProductId(product.getId());
        dto.setProductName(product.getName());
        dto.setProductCode(product.getCode());
        dto.setProductImageUrl(product.getImageUrl());
        dto.setStockQuantity(product.getStockQuantity());

        BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        dto.setUnitPrice(unitPrice);

        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        dto.setTotalPrice(lineTotal);

        boolean isActive = Boolean.TRUE.equals(product.getIsActive());
        Integer stock = product.getStockQuantity();

        if (!isActive) {
            dto.setAvailable(false);
            dto.setAvailabilityMessage("Məhsul artıq satışda deyil (deaktiv edilib).");
        } else if (stock == null || stock <= 0) {
            dto.setAvailable(false);
            dto.setAvailabilityMessage("Məhsul stokda bitib.");
        } else if (stock < quantity) {
            dto.setAvailable(false);
            dto.setAvailabilityMessage("Stokda cəmi " + stock + " ədəd mövcuddur (səbətdəki say: " + quantity + ").");
        } else {
            dto.setAvailable(true);
            dto.setAvailabilityMessage(null);
        }

        return dto;
    }
}
