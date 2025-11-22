package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherItemRequest;
import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherRequest;
import com.manhduc205.ezgear.dtos.request.voucher.SearchVoucherRequest;
import com.manhduc205.ezgear.dtos.request.voucher.VoucherRequest;
import com.manhduc205.ezgear.dtos.responses.voucher.ApplyVoucherResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.promotion.Promotion;
import com.manhduc205.ezgear.models.promotion.PromotionCategory;
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

    // CheckoutService gọi lại => TRÁNH GIAN LẬN FE !!!
    @Override
    public long calculateDiscountForCheckout(String voucherCode,
                                             List<ApplyVoucherItemRequest> items,
                                             long subtotal,
                                             long shippingFee) {

        return calculateDiscountCommon(voucherCode, items, subtotal, shippingFee);
    }

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

    @Override
    public Promotion createVoucher(VoucherRequest req) {
        if (promotionRepository.existsByCode(req.getCode())) {
            throw new RequestException("Mã voucher đã tồn tại");
        }

        Promotion promo = Promotion.builder()
                .code(req.getCode())
                .type(req.getType())
                .discountType(req.getDiscountType())
                .discountValue(req.getDiscountValue())
                .minOrder(req.getMinOrder())
                .maxDiscount(req.getMaxDiscount())
                .startAt(req.getStartAt())
                .endAt(req.getEndAt())
                .usageLimit(req.getUsageLimit())
                .usedCount(0)
                .scope(req.getScope())
                .status(req.getStatus())
                .build();

        Promotion saved = promotionRepository.save(promo);

        if ("CATEGORY".equals(req.getScope()) && req.getCategoryIds() != null) {
            for (Long categoryId : req.getCategoryIds()) {
                promotionCategoryRepository.save(
                        PromotionCategory.builder()
                                .promotionId(saved.getId())
                                .categoryId(categoryId)
                                .build()
                );
            }
        }

        return saved;
    }
    @Override
    public Promotion updateVoucher(Long id, VoucherRequest req) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new RequestException("Voucher không tồn tại"));

        // code không update
        promo.setType(req.getType());
        promo.setDiscountType(req.getDiscountType());
        promo.setDiscountValue(req.getDiscountValue());
        promo.setMinOrder(req.getMinOrder());
        promo.setMaxDiscount(req.getMaxDiscount());
        promo.setStartAt(req.getStartAt());
        promo.setEndAt(req.getEndAt());
        promo.setUsageLimit(req.getUsageLimit());
        promo.setScope(req.getScope());
        promo.setStatus(req.getStatus());

        Promotion updated = promotionRepository.save(promo);

        // cập nhật danh mục
        promotionCategoryRepository.deleteByPromotionId(id);

        if ("CATEGORY".equals(req.getScope()) && req.getCategoryIds() != null) {
            for (Long categoryId : req.getCategoryIds()) {
                promotionCategoryRepository.save(
                        PromotionCategory.builder()
                                .promotionId(id)
                                .categoryId(categoryId)
                                .build()
                );
            }
        }

        return updated;
    }

    @Override
    public void deleteVoucher(Long id) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new RequestException("Voucher không tồn tại"));

        // Xóa mapping category
        promotionCategoryRepository.deleteByPromotionId(id);
        promotionRepository.delete(promo);
    }
    @Override
    public List<Promotion> getAllVouchers() {
        return promotionRepository.findAll();
    }

    @Override
    public List<Promotion> searchVouchers(SearchVoucherRequest req) {

        List<Promotion> all = promotionRepository.findAll();

        return all.stream()
                // code LIKE
                .filter(v -> req.getCode() == null || req.getCode().isBlank() ||
                        v.getCode().toLowerCase().contains(req.getCode().toLowerCase()))

                // status
                .filter(v -> req.getStatus() == null ||
                        v.getStatus().equalsIgnoreCase(req.getStatus()))

                // type
                .filter(v -> req.getType() == null ||
                        v.getType().equalsIgnoreCase(req.getType()))

                // scope
                .filter(v -> req.getScope() == null ||
                        v.getScope().equalsIgnoreCase(req.getScope()))

                // discountType
                .filter(v -> req.getDiscountType() == null ||
                        v.getDiscountType().equalsIgnoreCase(req.getDiscountType()))

                // min order range
                .filter(v -> req.getMinOrderFrom() == null ||
                        v.getMinOrder() >= req.getMinOrderFrom())
                .filter(v -> req.getMinOrderTo() == null ||
                        v.getMinOrder() <= req.getMinOrderTo())

                // start date
                .filter(v -> req.getStartFrom() == null ||
                        !v.getStartAt().isBefore(req.getStartFrom()))
                .filter(v -> req.getStartTo() == null ||
                        !v.getStartAt().isAfter(req.getStartTo()))

                .toList();
    }

}


