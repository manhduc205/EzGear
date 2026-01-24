package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherRequest;
import com.manhduc205.ezgear.dtos.request.voucher.SearchVoucherRequest;
import com.manhduc205.ezgear.dtos.request.voucher.VoucherRequest;
import com.manhduc205.ezgear.dtos.responses.ApiResponse;
import com.manhduc205.ezgear.dtos.responses.voucher.ApplyVoucherResponse;
import com.manhduc205.ezgear.dtos.responses.voucher.PromotionResponse;
import com.manhduc205.ezgear.models.promotion.Promotion;
import com.manhduc205.ezgear.services.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/voucher")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping("/apply")
    public ResponseEntity<?> applyVoucher(@RequestBody ApplyVoucherRequest req) {
        try {
            ApplyVoucherResponse result = voucherService.applyVoucher(req);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Apply voucher success")
                    .payload(result)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<?> createVoucher(@RequestBody VoucherRequest req) {
        try {
            Promotion result = voucherService.createVoucher(req);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Create voucher success")
                    .payload(result)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateVoucher(@PathVariable Long id, @RequestBody VoucherRequest req) {
        try {
            Promotion result = voucherService.updateVoucher(id, req);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Update voucher success")
                    .payload(result)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVoucher(@PathVariable Long id) {
        try {
            voucherService.deleteVoucher(id);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Delete voucher success")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @GetMapping("")
    public ResponseEntity<?> getAll() {
        try {
            List<Promotion> list = voucherService.getAllVouchers();
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Get all vouchers success")
                    .payload(list)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping("/search")
    public ResponseEntity<?> searchVoucher(@RequestBody SearchVoucherRequest req) {
        try {
            List<Promotion> list = voucherService.searchVouchers(req);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Search vouchers success")
                    .payload(list)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableVouchers() {
        try {
            List<PromotionResponse> vouchers = voucherService.getAvailableVouchers();
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Get available vouchers success")
                    .payload(vouchers)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}