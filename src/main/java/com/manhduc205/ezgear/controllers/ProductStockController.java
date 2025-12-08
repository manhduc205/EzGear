package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.ProductStockDTO;
import com.manhduc205.ezgear.dtos.responses.StockResponse;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.ProductStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/stocks")
@RestController
@RequiredArgsConstructor
public class ProductStockController {
    private final ProductStockService productStockService;

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping("/adjust")
    public ResponseEntity<?> adjustStock(@RequestBody ProductStockDTO productStockDTO, @RequestParam int delta) {
        return ResponseEntity.ok(productStockService.adjustStock(productStockDTO, delta));
    }

    @GetMapping("/available")
    public ResponseEntity<?> availableStock(@RequestParam Long skuId, @RequestParam Long warehouseId) {
        int available = productStockService.getAvailable(skuId, warehouseId);
        return ResponseEntity.ok(available);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @GetMapping("")
    public ResponseEntity<?> getAllStock(@AuthenticationPrincipal CustomUserDetails user) {
        List<StockResponse> stocks = productStockService.getAllStock(user.getId());
        return ResponseEntity.ok(stocks);
    }
}
