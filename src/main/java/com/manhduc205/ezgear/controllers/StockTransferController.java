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
    public ResponseEntity<?> getAll(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(transferService.getAll(user.getId()));
    }

    @PostMapping
    public ResponseEntity<StockTransfer> createTransfer(@RequestBody StockTransferRequest req, @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(transferService.createTransfer(req, user.getId()));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<String> shipTransfer(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        transferService.shipTransfer(id, user.getId());
        return ResponseEntity.ok("Đã xuất kho chuyển đi.");
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<String> receiveTransfer( @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        transferService.receiveTransfer(id, user.getId());
        return ResponseEntity.ok("Đã nhập kho thành công.");
    }
    @PostMapping("/{id}/cancel")
    public ResponseEntity<String> cancelTransfer(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        transferService.cancelTransfer(id, user.getId());
        return ResponseEntity.ok("Đã hủy phiếu chuyển kho thành công.");
    }
}
