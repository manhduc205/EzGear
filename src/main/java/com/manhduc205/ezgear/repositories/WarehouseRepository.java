package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.Warehouse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findFirstByBranchIdAndIsActiveTrue(Long branchId);

    @Query("SELECT w FROM Warehouse w JOIN FETCH w.branch b " +
            "WHERE b.provinceId = :provinceId AND w.isActive = true AND b.isActive = true")
    List<Warehouse> findActiveWarehousesByProvince(@Param("provinceId") Integer provinceId);

}

