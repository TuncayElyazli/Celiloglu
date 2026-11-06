package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.dto.request.PaymentCreateRequestDto;
import com.celiloglu.backend.dto.response.PaymentResponseDto;
import com.celiloglu.backend.entity.Order;
import com.celiloglu.backend.entity.Payment;
import com.celiloglu.backend.entity.User;
import com.celiloglu.backend.enums.OrderStatus;
import com.celiloglu.backend.enums.PaymentMethod;
import com.celiloglu.backend.enums.PaymentStatus;
import com.celiloglu.backend.repository.OrderRepository;
import com.celiloglu.backend.repository.PaymentRepository;
import com.celiloglu.backend.service.PaymentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final EntityManager entityManager;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderRepository orderRepository,
                              EntityManager entityManager) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "PaymentRepository boş ola bilməz!");
        this.orderRepository = Objects.requireNonNull(orderRepository, "OrderRepository boş ola bilməz!");
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager boş ola bilməz!");
    }

    @Override
    @Transactional
    public PaymentResponseDto recordPayment(PaymentCreateRequestDto request, String recordedBy) {
        if (request == null) {
            throw new IllegalArgumentException("Ödəniş sorğusu boş ola bilməz!");
        }

        Long orderId = request.getOrderId();
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Sifariş ID-si müsbət ədəd olmalıdır!");
        }

        String cleanReceiptNumber = (request.getReceiptNumber() != null && !request.getReceiptNumber().trim().isEmpty())
                ? request.getReceiptNumber().trim() : null;
        if (cleanReceiptNumber == null) {
            throw new IllegalArgumentException("Qəbz nömrəsi qeyd edilməlidir!");
        }
        if (cleanReceiptNumber.length() > 64) {
            throw new IllegalArgumentException("Qəbz nömrəsi maksimum 64 simvol ola bilər!");
        }

        String cleanRecordedBy = (recordedBy != null && !recordedBy.trim().isEmpty())
                ? recordedBy.trim() : null;
        if (cleanRecordedBy == null) {
            throw new IllegalArgumentException("Qeydiyyatçı məlumatı təyin edilməlidir!");
        }
        if (cleanRecordedBy.length() > 100) {
            throw new IllegalArgumentException("Qeydiyyatçı məlumatı maksimum 100 simvol ola bilər!");
        }

        PaymentMethod method = request.getPaymentMethod();
        if (method == null) {
            throw new IllegalArgumentException("Ödəniş üsulu seçilməlidir!");
        }
        if (method != PaymentMethod.NAĞD && method != PaymentMethod.BANK_KÖÇÜRMƏSİ) {
            throw new IllegalArgumentException("Bu bölmədən yalnız faktiki daxil olmuş nağd və ya bank köçürməsi ödənişləri qəbul edilir!");
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ödəniş məbləği müsbət ədəd olmalıdır!");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Ödəniş məbləği qəpikdən artıq dəqiqlikdə ola bilməz!");
        }

        String cleanNotes = (request.getNotes() != null && !request.getNotes().trim().isEmpty())
                ? request.getNotes().trim() : null;
        if (cleanNotes != null && cleanNotes.length() > 255) {
            throw new IllegalArgumentException("Ödəniş qeydi maksimum 255 simvol ola bilər!");
        }

        // 1. Sifarişi PESSIMISTIC_WRITE ilə kilidlə və təzə məlumatı oxu
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Sifariş tapılmadı! ID: " + orderId));

        entityManager.refresh(order);

        // 2. İdempotentlik yoxlaması: Eyni qəbz nömrəsi ilə təkrar sorğu
        Optional<Payment> existingPaymentOpt = paymentRepository.findByReceiptNumber(cleanReceiptNumber);
        if (existingPaymentOpt.isPresent()) {
            Payment existing = existingPaymentOpt.get();
            boolean sameOrder = existing.getOrder() != null && orderId.equals(existing.getOrder().getId());
            boolean sameAmount = existing.getAmount() != null && existing.getAmount().compareTo(amount) == 0;
            boolean sameMethod = existing.getPaymentMethod() == method;
            boolean sameNotes = Objects.equals(existing.getNotes(), cleanNotes);
            boolean sameOperator = Objects.equals(existing.getRecordedBy(), cleanRecordedBy);

            if (sameOrder && sameAmount && sameMethod && sameNotes && sameOperator) {
                // Təkrar klik halında əvvəlki nəticəni sakitcə qaytar, yenidən pul yazma
                return mapToPaymentResponseDto(existing);
            }
            throw new IllegalStateException("Bu qəbz nömrəsi ilə artıq fərqli məlumatlara malik ödəniş qeydə alınıb: " + cleanReceiptNumber);
        }

        // 3. Sifariş statusunun yoxlanması: Ləğv edilmiş sifarişə ödəniş yazıla bilməz
        if (order.getStatus() == OrderStatus.LƏĞV_EDİLDİ) {
            throw new IllegalStateException("Ləğv edilmiş sifarişə ödəniş qəbul edilə bilməz!");
        }

        // 4. Kilid altında DB-dən təsdiqlənmiş ödənişlərin cəmini hesabla və qalıq borcu yoxla
        BigDecimal confirmedPaid = paymentRepository.sumAmountByOrderIdAndStatus(order.getId(), PaymentStatus.TAM_ÖDƏNİLMİŞ);
        if (confirmedPaid == null) {
            confirmedPaid = BigDecimal.ZERO;
        }

        BigDecimal totalOrderAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal remainingDebt = totalOrderAmount.subtract(confirmedPaid);
        if (remainingDebt.compareTo(BigDecimal.ZERO) < 0) {
            remainingDebt = BigDecimal.ZERO;
        }

        if (remainingDebt.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("Bu sifariş üzrə bütün məbləğ artıq tam ödənilib!");
        }

        if (amount.compareTo(remainingDebt) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Ödəniş məbləği (%.2f AZN) qalıq borcdan (%.2f AZN) çox ola bilməz!",
                    amount, remainingDebt
            ));
        }

        // 5. Müştəri şirkət adını sifariş sahibindən götür
        String customerCompany = extractCustomerCompany(order.getUser());

        // 6. Faktiki ödənişi yarat və saxla
        Payment payment = new Payment();
        payment.setReceiptNumber(cleanReceiptNumber);
        payment.setOrder(order);
        payment.setCustomerCompany(customerCompany);
        payment.setAmount(amount);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentMethod(method);
        payment.setStatus(PaymentStatus.TAM_ÖDƏNİLMİŞ);
        payment.setRecordedBy(cleanRecordedBy);
        payment.setNotes(cleanNotes);

        Payment savedPayment = paymentRepository.save(payment);
        if (order.getPayments() != null) {
            order.getPayments().add(savedPayment);
        }

        // 7. Sifarişin paymentStatus-unu təsdiqlənmiş yeni cəmə görə hesabla və yenilə
        BigDecimal newTotalPaid = confirmedPaid.add(amount);
        if (newTotalPaid.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentStatus(PaymentStatus.ÖDƏNİŞ_GÖZLƏYİR);
        } else if (newTotalPaid.compareTo(totalOrderAmount) < 0) {
            order.setPaymentStatus(PaymentStatus.QİSMƏN_ÖDƏNİLMİŞ);
        } else {
            order.setPaymentStatus(PaymentStatus.TAM_ÖDƏNİLMİŞ);
        }

        orderRepository.save(order);

        return mapToPaymentResponseDto(savedPayment);
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Sifariş ID-si müsbət ədəd olmalıdır!");
        }

        return paymentRepository.findByOrderIdOrderByPaymentDateDescIdDesc(orderId).stream()
                .map(this::mapToPaymentResponseDto)
                .toList();
    }

    @Override
    public List<PaymentResponseDto> getCustomerPaymentsByOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Sifariş ID-si müsbət ədəd olmalıdır!");
        }

        return paymentRepository.findByOrderIdOrderByPaymentDateDescIdDesc(orderId).stream()
                .map(this::mapToCustomerPaymentResponseDto)
                .toList();
    }

    @Override
    public BigDecimal getPaidAmountByOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Sifariş ID-si müsbət ədəd olmalıdır!");
        }

        BigDecimal sum = paymentRepository.sumAmountByOrderIdAndStatus(orderId, PaymentStatus.TAM_ÖDƏNİLMİŞ);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public Page<PaymentResponseDto> getAllPaymentsForAdmin(Pageable pageable) {
        return getPaymentsForAdmin(null, null, pageable);
    }

    @Override
    public Page<PaymentResponseDto> getPaymentsForAdmin(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Sort requiredSort = Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id"));
        Pageable effectivePageable;
        if (pageable == null) {
            effectivePageable = PageRequest.of(0, 10, requiredSort);
        } else {
            int pageNumber = Math.max(pageable.getPageNumber(), 0);
            int pageSize = pageable.getPageSize() > 0 ? pageable.getPageSize() : 10;
            effectivePageable = PageRequest.of(pageNumber, pageSize, requiredSort);
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Başlanğıc tarixi bitiş tarixindən sonra ola bilməz.");
        }

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;

        Page<Payment> paymentsPage;
        if (startDateTime != null && endDateTime != null) {
            paymentsPage = paymentRepository.findByPaymentDateGreaterThanEqualAndPaymentDateLessThan(startDateTime, endDateTime, effectivePageable);
        } else if (startDateTime != null) {
            paymentsPage = paymentRepository.findByPaymentDateGreaterThanEqual(startDateTime, effectivePageable);
        } else if (endDateTime != null) {
            paymentsPage = paymentRepository.findByPaymentDateLessThan(endDateTime, effectivePageable);
        } else {
            paymentsPage = paymentRepository.findAllBy(effectivePageable);
        }

        return paymentsPage.map(this::mapToPaymentResponseDto);
    }

    @Override
    public String generateReceiptNumber() {
        String receiptNumber;
        do {
            receiptNumber = "RCP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (paymentRepository.findByReceiptNumber(receiptNumber).isPresent());
        return receiptNumber;
    }

    private String extractCustomerCompany(User user) {
        if (user == null) {
            return "-";
        }
        if (user.getCompanyName() != null && !user.getCompanyName().trim().isEmpty()) {
            return user.getCompanyName().trim();
        }
        String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }
        return user.getUsername() != null ? user.getUsername() : "-";
    }

    private PaymentResponseDto mapToPaymentResponseDto(Payment payment) {
        if (payment == null) {
            return null;
        }

        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setId(payment.getId());
        dto.setReceiptNumber(payment.getReceiptNumber());
        if (payment.getOrder() != null) {
            dto.setOrderId(payment.getOrder().getId());
            dto.setOrderNumber(payment.getOrder().getOrderNumber());
        }
        dto.setCustomerCompany(payment.getCustomerCompany());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setStatus(payment.getStatus());
        dto.setRecordedBy(payment.getRecordedBy());
        dto.setNotes(payment.getNotes());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }

    private PaymentResponseDto mapToCustomerPaymentResponseDto(Payment payment) {
        if (payment == null) {
            return null;
        }

        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setId(payment.getId());
        dto.setReceiptNumber(payment.getReceiptNumber());
        if (payment.getOrder() != null) {
            dto.setOrderId(payment.getOrder().getId());
            dto.setOrderNumber(payment.getOrder().getOrderNumber());
        }
        dto.setCustomerCompany(payment.getCustomerCompany());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setStatus(payment.getStatus());
        // Müştəriyə recordedBy və kassirin daxili qeydləri doldurulmur və göstərilmir
        dto.setRecordedBy(null);
        dto.setNotes(null);
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }
}

