package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    List<Warehouse> findFirstByBranchIdAndIsActiveTrue(Long branchId);
}
