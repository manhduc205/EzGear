package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherItemRequest;
import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherRequest;
import com.manhduc205.ezgear.dtos.request.voucher.SearchVoucherRequest;
import com.manhduc205.ezgear.dtos.request.voucher.VoucherRequest;
import com.manhduc205.ezgear.dtos.responses.voucher.ApplyVoucherResponse;
import com.manhduc205.ezgear.dtos.responses.voucher.PromotionResponse;
import com.manhduc205.ezgear.exceptions.DataNotFoundException;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.promotion.Promotion;
import com.manhduc205.ezgear.models.promotion.PromotionCategory;
import com.manhduc205.ezgear.models.promotion.PromotionUsage;
import com.manhduc205.ezgear.repositories.PromotionCategoryRepository;
import com.manhduc205.ezgear.repositories.PromotionRepository;
import com.manhduc205.ezgear.repositories.PromotionUsageRepository;
import com.manhduc205.ezgear.services.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final PromotionRepository promotionRepository;
    private final PromotionCategoryRepository promotionCategoryRepository;
    private final PromotionUsageRepository promotionUsageRepository;
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
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.voucher.not_found")));

        validatePromotion(promo);
        validateMinOrder(promo, subtotal);

        return calculateDiscountInternal(items, subtotal, shippingFee, promo);
    }

    private void validatePromotion(Promotion promo){
        if(!"ACTIVE".equals(promo.getStatus()))
            throw new RequestException(Translator.toLocale("error.voucher.inactive"));

        LocalDateTime now = LocalDateTime.now();
        if(now.isBefore(promo.getStartAt()) || now.isAfter(promo.getEndAt()))
            throw new RequestException(Translator.toLocale("error.voucher.expired"));

        if(promo.getUsageLimit() != null && promo.getUsageLimit() > 0 && promo.getUsedCount() >= promo.getUsageLimit()) {
            throw new RequestException(Translator.toLocale("error.voucher.usage_exceeded"));
        }
    }

    private void validateMinOrder(Promotion promo, Long subtotal){
        if(subtotal < promo.getMinOrder())
            throw new RequestException(Translator.toLocale("error.voucher.min_order_not_met"));
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
    @Transactional
    public Promotion createVoucher(VoucherRequest req) {
        if (promotionRepository.existsByCode(req.getCode())) {
            throw new RequestException(Translator.toLocale("error.voucher.code_exists"));
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
    @Transactional
    public Promotion updateVoucher(Long id, VoucherRequest req) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.voucher.not_found")));

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
    @Transactional
    public void deleteVoucher(Long id) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.voucher.not_found")));

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

    @Override
    public List<PromotionResponse> getAvailableVouchers() {
        List<Promotion> entities = promotionRepository.findAvailableVouchers();

        return entities.stream().map(p -> {
            List<Long> catIds = null;

            // Nếu là voucher theo danh mục, phải lấy danh sách ID danh mục kèm theo
            if ("CATEGORY".equals(p.getScope())) {
                catIds = promotionCategoryRepository.findCategoryIdsByPromotionId(p.getId());
            }

            return PromotionResponse.builder()
                    .id(p.getId())
                    .code(p.getCode())
                    .type(p.getType())
                    .discountType(p.getDiscountType())
                    .discountValue(p.getDiscountValue())
                    .maxDiscount(p.getMaxDiscount())
                    .minOrder(p.getMinOrder())
                    .scope(p.getScope())
                    .startAt(p.getStartAt())
                    .endAt(p.getEndAt())
                    .applicableCategoryIds(catIds)
                    .build();
        }).toList();
    }
    @Override
    @Transactional
    public void recordVoucherUsage(String code, Long userId, Long orderId) {
        Promotion promotion = promotionRepository.findByCode(code)
                .orElseThrow(() -> new DataNotFoundException("Voucher not found"));

        // Double check phút chót
        validatePromotion(promotion);

        // Tăng lượt dùng
        promotion.setUsedCount(promotion.getUsedCount() + 1);
        promotionRepository.save(promotion);

        PromotionUsage usage = PromotionUsage.builder()
                .promotionId(promotion.getId())
                .userId(userId)
                .orderId(orderId)
                .usedAt(LocalDateTime.now())
                .build();
        promotionUsageRepository.save(usage);
    }
}
