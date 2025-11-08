package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByCode(String code);
    Optional<Order> findByCode(String code);
}
