package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.StockTransferRequest;
import com.manhduc205.ezgear.dtos.responses.StockTransferResponse;
import com.manhduc205.ezgear.models.StockTransfer;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.StockTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock-transfers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
public class StockTransferController {

    private final StockTransferService transferService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(transferService.getAll());
    }

    @PostMapping
    public ResponseEntity<StockTransfer> createTransfer(
            @RequestBody StockTransferRequest req,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(transferService.createTransfer(req, user.getId()));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<String> shipTransfer(@PathVariable Long id) {
        transferService.shipTransfer(id);
        return ResponseEntity.ok("Đã xuất kho chuyển đi.");
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<String> receiveTransfer(@PathVariable Long id) {
        transferService.receiveTransfer(id);
        return ResponseEntity.ok("Đã nhập kho thành công.");
    }
}
