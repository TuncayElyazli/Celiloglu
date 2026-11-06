package com.celiloglu.backend.repository;

import com.celiloglu.backend.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(ci.quantity), 0L) FROM CartItem ci WHERE ci.cart.user.id = :userId")
    Long sumQuantityByUserId(@Param("userId") Long userId);
}
