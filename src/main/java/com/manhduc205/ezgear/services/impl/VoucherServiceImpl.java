package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherItemRequest;
import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherRequest;
import com.manhduc205.ezgear.dtos.responses.voucher.ApplyVoucherResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.promotion.Promotion;
import com.manhduc205.ezgear.repositories.PromotionCategoryRepository;
import com.manhduc205.ezgear.repositories.PromotionRepository;
import com.manhduc205.ezgear.services.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final PromotionRepository promotionRepository;
    private final PromotionCategoryRepository promotionCategoryRepository;

    // ===========================================
    // FE gọi /voucher/apply => dùng method này
    // ===========================================
    @Override
    public ApplyVoucherResponse applyVoucher(ApplyVoucherRequest req) {

        long discount = calculateDiscountCommon(
                req.getCode(),
                req.getItems(),
                req.getSubtotal(),
                req.getShippingFee()
        );

        return ApplyVoucherResponse.builder()
                .code(req.getCode())
                .discount(discount)
                .build();
    }

    // =================================================
    // CheckoutService gọi lại => TRÁNH GIAN LẬN FE !!!
    // =================================================
    @Override
    public long calculateDiscountForCheckout(String voucherCode,
                                             List<ApplyVoucherItemRequest> items,
                                             long subtotal,
                                             long shippingFee) {

        return calculateDiscountCommon(voucherCode, items, subtotal, shippingFee);
    }


    // =================================================================
    // HÀM CHUNG — FE apply và CheckoutService đều dùng cùng 1 logic
    // =================================================================
    private long calculateDiscountCommon(String code,
                                         List<ApplyVoucherItemRequest> items,
                                         long subtotal,
                                         long shippingFee) {

        Promotion promo = promotionRepository.findByCode(code)
                .orElseThrow(() -> new RequestException("Mã giảm giá không tồn tại."));

        validatePromotion(promo);
        validateMinOrder(promo, subtotal);

        return calculateDiscountInternal(items, subtotal, shippingFee, promo);
    }


    // ===============================
    // VALIDATION
    // ===============================
    private void validatePromotion(Promotion promo){
        if(!"ACTIVE".equals(promo.getStatus()))
            throw new RequestException("Voucher không khả dụng.");

        LocalDateTime now = LocalDateTime.now();
        if(now.isBefore(promo.getStartAt()) || now.isAfter(promo.getEndAt()))
            throw new RequestException("Voucher đã hết hạn.");

        if(promo.getUsageLimit() > 0 && promo.getUsedCount() >= promo.getUsageLimit())
            throw new RequestException("Voucher đã hết lượt sử dụng.");
    }

    private void validateMinOrder(Promotion promo, Long subtotal){
        if(subtotal < promo.getMinOrder())
            throw new RequestException("Đơn hàng không đủ điều kiện tối thiểu.");
    }


    // ===============================
    // LOGIC TÍNH GIẢM GIÁ
    // ===============================
    private long calculateDiscountInternal(List<ApplyVoucherItemRequest> items,
                                           long subtotal,
                                           long shippingFee,
                                           Promotion promo){

        // FREE SHIPPING
        if("SHIPPING".equals(promo.getType())){
            return Math.min(shippingFee, promo.getDiscountValue());
        }

        // GIẢM THEO TOÀN ĐƠN
        if("ALL".equals(promo.getScope())){

            if("AMOUNT".equals(promo.getDiscountType())){
                return promo.getDiscountValue();
            }

            if("PERCENT".equals(promo.getDiscountType())){
                long discount = subtotal * promo.getDiscountValue() / 100;
                if(promo.getMaxDiscount() != null)
                    discount = Math.min(discount, promo.getMaxDiscount());
                return discount;
            }
        }

        // GIẢM THEO DANH MỤC
        if("CATEGORY".equals(promo.getScope())){

            List<Long> allowedCats =
                    promotionCategoryRepository.findCategoryIdsByPromotionId(promo.getId());

            long categorySum = 0;

            for(ApplyVoucherItemRequest item : items){
                if(allowedCats.contains(item.getCategoryId())){
                    categorySum += item.getPrice() * item.getQuantity();
                }
            }

            if(categorySum == 0) return 0;

            long discount = categorySum * promo.getDiscountValue() / 100;

            if(promo.getMaxDiscount() != null)
                discount = Math.min(discount, promo.getMaxDiscount());

            return discount;
        }

        return 0;
    }
}


