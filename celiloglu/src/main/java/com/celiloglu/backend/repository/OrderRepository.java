package com.celiloglu.backend.repository;

import com.celiloglu.backend.entity.Order;
import com.celiloglu.backend.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(OrderStatus status);
    long countByStatus(OrderStatus status);

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    List<Order> findAllBy(Sort sort);

    Page<Order> findByUserId(Long userId, Pageable pageable);
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.user.id = :userId")
    Optional<Order> findByIdAndUserIdWithLock(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") Long id);

    @Query(value = "SELECT " +
            "COALESCE(SUM(os.total_amount), 0) AS total_order_amount, " +
            "COALESCE(SUM(os.order_paid), 0) AS total_paid_amount, " +
            "COALESCE(SUM(os.order_remaining_debt), 0) AS total_remaining_debt " +
            "FROM (" +
            "  SELECT " +
            "    o.id AS order_id, " +
            "    o.user_id, " +
            "    o.total_amount, " +
            "    COALESCE(p_agg.paid_amount, 0) AS order_paid, " +
            "    CASE " +
            "      WHEN (o.total_amount - COALESCE(p_agg.paid_amount, 0)) > 0 " +
            "      THEN (o.total_amount - COALESCE(p_agg.paid_amount, 0)) " +
            "      ELSE 0 " +
            "    END AS order_remaining_debt " +
            "  FROM orders o " +
            "  LEFT JOIN (" +
            "    SELECT p.order_id, SUM(p.amount) AS paid_amount " +
            "    FROM payments p " +
            "    WHERE p.status = :paidStatus " +
            "    GROUP BY p.order_id " +
            "  ) p_agg ON o.id = p_agg.order_id " +
            "  WHERE o.status <> :cancelledStatus " +
            "    AND o.user_id IS NOT NULL " +
            ") os", nativeQuery = true)
    List<Object[]> getB2BAnalyticsSummary(
            @Param("cancelledStatus") String cancelledStatus,
            @Param("paidStatus") String paidStatus);

    @Query(value = "SELECT " +
            "u.id AS user_id, " +
            "u.username AS username, " +
            "u.first_name AS first_name, " +
            "u.last_name AS last_name, " +
            "u.company_name AS company_name, " +
            "COUNT(os.order_id) AS order_count, " +
            "SUM(os.total_amount) AS total_order_amount, " +
            "SUM(os.order_paid) AS total_paid_amount, " +
            "SUM(os.order_remaining_debt) AS remaining_debt " +
            "FROM (" +
            "  SELECT " +
            "    o.id AS order_id, " +
            "    o.user_id, " +
            "    o.total_amount, " +
            "    COALESCE(p_agg.paid_amount, 0) AS order_paid, " +
            "    CASE " +
            "      WHEN (o.total_amount - COALESCE(p_agg.paid_amount, 0)) > 0 " +
            "      THEN (o.total_amount - COALESCE(p_agg.paid_amount, 0)) " +
            "      ELSE 0 " +
            "    END AS order_remaining_debt " +
            "  FROM orders o " +
            "  LEFT JOIN (" +
            "    SELECT p.order_id, SUM(p.amount) AS paid_amount " +
            "    FROM payments p " +
            "    WHERE p.status = :paidStatus " +
            "    GROUP BY p.order_id " +
            "  ) p_agg ON o.id = p_agg.order_id " +
            "  WHERE o.status <> :cancelledStatus " +
            "    AND o.user_id IS NOT NULL " +
            ") os " +
            "JOIN users u ON u.id = os.user_id " +
            "GROUP BY u.id, u.username, u.first_name, u.last_name, u.company_name " +
            "ORDER BY SUM(os.order_remaining_debt) DESC, u.id DESC",
           countQuery = "SELECT COUNT(DISTINCT o.user_id) FROM orders o JOIN users u ON u.id = o.user_id WHERE o.status <> :cancelledStatus",
           nativeQuery = true)
    Page<Object[]> findB2BCustomerAnalytics(
            @Param("cancelledStatus") String cancelledStatus,
            @Param("paidStatus") String paidStatus,
            Pageable pageable);

    @Query(value = "SELECT " +
            "u.id AS user_id, " +
            "u.username AS username, " +
            "u.first_name AS first_name, " +
            "u.last_name AS last_name, " +
            "u.company_name AS company_name, " +
            "COUNT(os.order_id) AS order_count, " +
            "SUM(os.total_amount) AS total_order_amount, " +
            "SUM(os.order_paid) AS total_paid_amount, " +
            "SUM(os.order_remaining_debt) AS remaining_debt " +
            "FROM (" +
            "  SELECT " +
            "    o.id AS order_id, " +
            "    o.user_id, " +
            "    o.total_amount, " +
            "    COALESCE(p_agg.paid_amount, 0) AS order_paid, " +
            "    CASE " +
            "      WHEN (o.total_amount - COALESCE(p_agg.paid_amount, 0)) > 0 " +
            "      THEN (o.total_amount - COALESCE(p_agg.paid_amount, 0)) " +
            "      ELSE 0 " +
            "    END AS order_remaining_debt " +
            "  FROM orders o " +
            "  LEFT JOIN (" +
            "    SELECT p.order_id, SUM(p.amount) AS paid_amount " +
            "    FROM payments p " +
            "    WHERE p.status = :paidStatus " +
            "    GROUP BY p.order_id " +
            "  ) p_agg ON o.id = p_agg.order_id " +
            "  WHERE o.status <> :cancelledStatus " +
            "    AND o.user_id IS NOT NULL " +
            ") os " +
            "JOIN users u ON u.id = os.user_id " +
            "GROUP BY u.id, u.username, u.first_name, u.last_name, u.company_name " +
            "ORDER BY SUM(os.order_remaining_debt) DESC, u.id DESC",
           nativeQuery = true)
    List<Object[]> findAllB2BCustomerAnalytics(
            @Param("cancelledStatus") String cancelledStatus,
            @Param("paidStatus") String paidStatus);
}
