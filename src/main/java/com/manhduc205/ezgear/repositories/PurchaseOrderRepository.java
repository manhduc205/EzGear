package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.dtos.PurchaseOrderDTO;
import com.manhduc205.ezgear.models.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByCode(String code);
}
