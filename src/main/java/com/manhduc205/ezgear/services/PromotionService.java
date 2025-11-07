package com.manhduc205.ezgear.services;

import java.math.BigDecimal;

public interface PromotionService {
    BigDecimal applyVoucher(String code, BigDecimal subtotal);
}
