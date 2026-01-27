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
    private String skuCode;
    private Integer quantity;

    private Long  unitPrice;
}
