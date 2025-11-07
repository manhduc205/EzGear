package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.models.Promotion;
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
    public BigDecimal applyVoucher(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) return BigDecimal.ZERO;

        Promotion promo = promotionRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));

        // Kiểm tra trạng thái
        if (!"ACTIVE".equalsIgnoreCase(promo.getStatus())) {
            throw new RuntimeException("Mã giảm giá không còn hoạt động");
        }

        // Kiểm tra thời gian
        LocalDateTime now = LocalDateTime.now();
        if (promo.getStartAt() != null && now.isBefore(promo.getStartAt())) {
            throw new RuntimeException("Mã giảm giá chưa bắt đầu");
        }
        if (promo.getEndAt() != null && now.isAfter(promo.getEndAt())) {
            throw new RuntimeException("Mã giảm giá đã hết hạn");
        }

        // Kiểm tra giới hạn sử dụng
        if (promo.getUsageLimit() != null && promo.getUsedCount() != null &&
                promo.getUsedCount() >= promo.getUsageLimit()) {
            throw new RuntimeException("Mã giảm giá đã đạt giới hạn sử dụng");
        }

        // Kiểm tra giá trị tối thiểu
        if (promo.getMinOrder() != null && subtotal.compareTo(promo.getMinOrder()) < 0) {
            throw new RuntimeException("Đơn hàng không đủ điều kiện sử dụng mã giảm giá");
        }

        BigDecimal discount = calculateDiscount(promo, subtotal);

        // Nếu có max_discount → giới hạn lại
        if (promo.getMaxDiscount() != null && discount.compareTo(promo.getMaxDiscount()) > 0) {
            discount = promo.getMaxDiscount();
        }

        // Tăng used_count
        promo.setUsedCount(promo.getUsedCount() == null ? 1 : promo.getUsedCount() + 1);
        promotionRepository.save(promo);

        return discount;
    }

    private BigDecimal calculateDiscount(Promotion promo, BigDecimal subtotal) {
        if ("PERCENT".equalsIgnoreCase(promo.getDiscountType())) {
            // giảm theo %
            return subtotal.multiply(promo.getDiscountValue()).divide(BigDecimal.valueOf(100));
        } else if ("FIXED".equalsIgnoreCase(promo.getDiscountType())) {
            // giảm theo số tiền cố định
            return promo.getDiscountValue();
        } else {
            return BigDecimal.ZERO;
        }
    }
}
