package com.manhduc205.ezgear.dtos.responses;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class OrderItemResponse {
    private Long skuId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
}
