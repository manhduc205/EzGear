package com.manhduc205.ezgear.dtos.request;

import lombok.*;
import java.math.BigDecimal;

/**
 * 🧺 Thông tin từng sản phẩm trong đơn hàng,
 * được lưu snapshot từ thời điểm checkout.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {
    private Long skuId;
    private String productNameSnapshot;
    private String skuNameSnapshot;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
}
