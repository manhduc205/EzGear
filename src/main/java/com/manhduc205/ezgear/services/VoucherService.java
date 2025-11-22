package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherItemRequest;
import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherRequest;
import com.manhduc205.ezgear.dtos.responses.voucher.ApplyVoucherResponse;

import java.util.List;

public interface VoucherService {
    ApplyVoucherResponse applyVoucher(ApplyVoucherRequest req);
    long calculateDiscountForCheckout(String voucherCode,
                                      List<ApplyVoucherItemRequest> items,
                                      long subtotal,
                                      long shippingFee);
}