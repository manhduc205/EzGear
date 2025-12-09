package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByCode(String code);

    // Tìm hóa đơn theo ID đơn hàng (Mỗi đơn hàng chỉ có 1 hóa đơn active)
    @Query("SELECT i FROM Invoice i WHERE i.order.id = :orderId AND i.status != 'CANCELLED'")
    Optional<Invoice> findByOrderId(@Param("orderId") Long orderId);

    boolean existsByCode(String code);
}