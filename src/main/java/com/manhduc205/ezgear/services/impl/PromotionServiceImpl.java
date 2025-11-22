package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.models.promotion.Promotion;
import com.manhduc205.ezgear.repositories.PromotionRepository;
import com.manhduc205.ezgear.services.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    @Override
    public Long applyVoucher(String code, Long subtotal) {
        if (code == null || code.isBlank()) return 0L;

        Promotion promo = promotionRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));

        // 1. Kiểm tra trạng thái
        if (!"ACTIVE".equalsIgnoreCase(promo.getStatus())) {
            throw new RuntimeException("Mã giảm giá không còn hoạt động");
        }

        // 2. Kiểm tra thời gian
        LocalDateTime now = LocalDateTime.now();

        if (promo.getStartAt() != null && now.isBefore(promo.getStartAt())) {
            throw new RuntimeException("Mã giảm giá chưa bắt đầu");
        }

        if (promo.getEndAt() != null && now.isAfter(promo.getEndAt())) {
            throw new RuntimeException("Mã giảm giá đã hết hạn");
        }

        // 3. Kiểm tra giới hạn sử dụng
        if (promo.getUsageLimit() != null && promo.getUsedCount() != null &&
                promo.getUsedCount() >= promo.getUsageLimit()) {
            throw new RuntimeException("Mã giảm giá đã đạt giới hạn sử dụng");
        }

        // 4. Kiểm tra giá trị tối thiểu
        if (promo.getMinOrder() != null && subtotal < promo.getMinOrder()) {
            throw new RuntimeException("Đơn hàng không đủ điều kiện áp mã giảm giá");
        }

        // 5. Tính giảm giá
        Long discount = calculateDiscount(promo, subtotal);

        // 6. Giới hạn theo max_discount nếu có
        if (promo.getMaxDiscount() != null && discount > promo.getMaxDiscount()) {
            discount = promo.getMaxDiscount();
        }

        return discount; // ⚠️ Không cập nhật used_count tại đây!
    }

    private Long calculateDiscount(Promotion promo, Long subtotal) {

        String discountType = promo.getDiscountType();

        if ("PERCENT".equalsIgnoreCase(discountType)) {
            // giảm theo %
            return subtotal * promo.getDiscountValue() / 100;
        }

        if ("AMOUNT".equalsIgnoreCase(discountType) ||
                "FIXED".equalsIgnoreCase(discountType)) {
            // giảm số tiền cố định
            return promo.getDiscountValue();
        }

        return 0L;
    }
}

