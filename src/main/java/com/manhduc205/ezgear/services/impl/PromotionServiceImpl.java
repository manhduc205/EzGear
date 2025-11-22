package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.promotion.Promotion;
import com.manhduc205.ezgear.repositories.PromotionRepository;
import com.manhduc205.ezgear.services.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    @Override
    public Long applyVoucher(String code, Long subtotal) {
        if (code == null || code.isBlank()) return 0L;

        Promotion promo = promotionRepository.findByCode(code)
                .orElseThrow(() -> new RequestException("Mã giảm giá không tồn tại"));

        // 1. Kiểm tra trạng thái
        if (!"ACTIVE".equalsIgnoreCase(promo.getStatus())) {
            throw new RequestException("Mã giảm giá không còn hoạt động");
        }

        // 2. Kiểm tra thời gian
        LocalDateTime now = LocalDateTime.now();

        if (promo.getStartAt() != null && now.isBefore(promo.getStartAt())) {
            throw new RequestException("Mã giảm giá chưa bắt đầu");
        }

        if (promo.getEndAt() != null && now.isAfter(promo.getEndAt())) {
            throw new RequestException("Mã giảm giá đã hết hạn");
        }

        // 3. Kiểm tra giới hạn sử dụng
        if (promo.getUsageLimit() != null && promo.getUsedCount() != null &&
                promo.getUsedCount() >= promo.getUsageLimit()) {
            throw new RequestException("Mã giảm giá đã đạt giới hạn sử dụng");
        }

        // 4. Kiểm tra giá trị tối thiểu
        if (promo.getMinOrder() != null && subtotal < promo.getMinOrder()) {
            throw new RequestException("Đơn hàng không đủ điều kiện sử dụng mã giảm giá");
        }

        // 5. Tính mức giảm
        Long discount = calculateDiscount(promo, subtotal);

        // 6. Áp dụng mức giảm tối đa
        if (promo.getMaxDiscount() != null && discount > promo.getMaxDiscount()) {
            discount = promo.getMaxDiscount();
        }

        return discount;
    }

    private Long calculateDiscount(Promotion promo, Long subtotal) {

        String discountType = promo.getDiscountType();

        if ("PERCENT".equalsIgnoreCase(discountType)) {
            return subtotal * promo.getDiscountValue() / 100;
        }

        if ("AMOUNT".equalsIgnoreCase(discountType) ||
                "FIXED".equalsIgnoreCase(discountType)) {
            return promo.getDiscountValue();
        }

        return 0L;
    }
}
