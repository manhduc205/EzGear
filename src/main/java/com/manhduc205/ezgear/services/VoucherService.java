package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherItemRequest;
import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherRequest;
import com.manhduc205.ezgear.dtos.request.voucher.SearchVoucherRequest;
import com.manhduc205.ezgear.dtos.request.voucher.VoucherRequest;
import com.manhduc205.ezgear.dtos.responses.voucher.ApplyVoucherResponse;
import com.manhduc205.ezgear.dtos.responses.voucher.PromotionResponse;
import com.manhduc205.ezgear.models.promotion.Promotion;

import java.util.List;

public interface VoucherService {
    ApplyVoucherResponse applyVoucher(ApplyVoucherRequest req);
    long calculateDiscountForCheckout(String voucherCode,
                                      List<ApplyVoucherItemRequest> items,
                                      long subtotal,
                                      long shippingFee);
    Promotion createVoucher(VoucherRequest req);

    Promotion updateVoucher(Long id,VoucherRequest req);

    void deleteVoucher(Long id);
    List<Promotion> getAllVouchers();
    List<Promotion> searchVouchers(SearchVoucherRequest req);
    List<PromotionResponse> getAvailableVouchers();
    void recordVoucherUsage(String code, Long userId, Long orderId);
}