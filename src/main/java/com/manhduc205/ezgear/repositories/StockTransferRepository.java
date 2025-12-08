package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.StockTransfer;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    Optional<StockTransfer> findByCode(String code);
    @Query("SELECT st FROM StockTransfer st " +
            "WHERE st.fromWarehouse.branch.id = :branchId " +
            "OR st.toWarehouse.branch.id = :branchId " +
            "ORDER BY st.createdAt DESC")
    List<StockTransfer> findAllByBranchId(@Param("branchId") Long branchId);
}

