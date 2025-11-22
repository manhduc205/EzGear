package com.manhduc205.ezgear.services;

import java.math.BigDecimal;

public interface PromotionService {
    Long applyVoucher(String code, Long subtotal);
}
