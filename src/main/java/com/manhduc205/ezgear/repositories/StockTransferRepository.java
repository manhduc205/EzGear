package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    Optional<StockTransfer> findByCode(String code);
}

