package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherRequest;
import com.manhduc205.ezgear.dtos.responses.voucher.ApplyVoucherResponse;
import com.manhduc205.ezgear.services.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/voucher")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping("/apply")
    public ApplyVoucherResponse applyVoucher(@RequestBody ApplyVoucherRequest req) {
        return voucherService.applyVoucher(req);
    }
}

