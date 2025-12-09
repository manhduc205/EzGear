package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.StockTransactionReportDTO;
import com.manhduc205.ezgear.services.StockTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock-transactions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
public class StockTransactionController {

    private final StockTransactionService stockTransactionService;

    @GetMapping
    public ResponseEntity<List<StockTransactionReportDTO>> getTransactions(
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long warehouseId) {
        return ResponseEntity.ok(stockTransactionService.getTransactionReports(skuId, warehouseId));
    }
}
