package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

}
