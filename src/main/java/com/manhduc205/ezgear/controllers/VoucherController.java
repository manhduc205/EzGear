package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherRequest;
import com.manhduc205.ezgear.dtos.request.voucher.SearchVoucherRequest;
import com.manhduc205.ezgear.dtos.request.voucher.VoucherRequest;
import com.manhduc205.ezgear.dtos.responses.voucher.ApplyVoucherResponse;
import com.manhduc205.ezgear.models.promotion.Promotion;
import com.manhduc205.ezgear.services.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/voucher")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping("/apply")
    public ApplyVoucherResponse applyVoucher(@RequestBody ApplyVoucherRequest req) {
        return voucherService.applyVoucher(req);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping("/create")
    public Promotion createVoucher(@RequestBody VoucherRequest req) {
        return voucherService.createVoucher(req);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PutMapping("/{id}")
    public Promotion updateVoucher(@PathVariable Long id, @RequestBody VoucherRequest req) {
        return voucherService.updateVoucher(id, req);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @GetMapping("")
    public List<Promotion> getAll() {
        return voucherService.getAllVouchers();
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping("/search")
    public List<Promotion> searchVoucher(@RequestBody SearchVoucherRequest req) {
        return voucherService.searchVouchers(req);
    }
}
