package com.celiloglu.backend.repository;

import com.celiloglu.backend.entity.Payment;
import com.celiloglu.backend.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReceiptNumber(String receiptNumber);
    List<Payment> findByOrderId(Long orderId);
    List<Payment> findByOrderIdOrderByPaymentDateDescIdDesc(Long orderId);
    List<Payment> findByStatus(PaymentStatus status);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Page<Payment> findByPaymentDateGreaterThanEqualAndPaymentDateLessThan(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Page<Payment> findByPaymentDateGreaterThanEqual(LocalDateTime start, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Page<Payment> findByPaymentDateLessThan(LocalDateTime end, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "order.user"})
    List<Payment> findByPaymentDateGreaterThanEqualAndPaymentDateLessThan(LocalDateTime start, LocalDateTime end, Sort sort);

    @EntityGraph(attributePaths = {"order", "order.user"})
    List<Payment> findByPaymentDateGreaterThanEqual(LocalDateTime start, Sort sort);

    @EntityGraph(attributePaths = {"order", "order.user"})
    List<Payment> findByPaymentDateLessThan(LocalDateTime end, Sort sort);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Page<Payment> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"order", "order.user"})
    List<Payment> findAllBy(Sort sort);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.order.id = :orderId AND p.status = :status")
    BigDecimal sumAmountByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT cast(p.paymentDate as date), COALESCE(SUM(p.amount), 0) " +
           "FROM Payment p " +
           "WHERE p.status = :status AND p.paymentDate >= :startDate AND p.paymentDate < :endDate " +
           "GROUP BY cast(p.paymentDate as date) " +
           "ORDER BY cast(p.paymentDate as date) ASC")
    List<Object[]> sumDailyAmountByStatusAndDateRange(
            @Param("status") PaymentStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
