package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.order.Order;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByCode(String code);
    Optional<Order> findByCode(String code);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // lấy đơn hàng để lấy hàng (picking) theo chi nhánh và trạng thái
    @Query("SELECT o FROM Order o " +
            "WHERE o.branchId = :branchId " +
            "AND o.status IN :statuses " +
            "ORDER BY o.createdAt ASC")
    List<Order> findForPicking(@Param("branchId") Long branchId,
                               @Param("statuses") List<String> statuses);
}
