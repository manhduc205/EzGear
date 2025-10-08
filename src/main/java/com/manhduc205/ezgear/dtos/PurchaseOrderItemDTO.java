package com.manhduc205.ezgear.dtos;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItemDTO {

    private Long id;

    private Long purchaseOrderId;

    private Long skuId;

    private Integer quantity;

    private BigDecimal unitPrice;
}
