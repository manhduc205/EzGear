package com.manhduc205.ezgear.dtos.responses;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponse {

    private Long id;
    private String code;
    private String supplierName;

    private String warehouseName;
    private String status;

    private BigDecimal subtotal;
    private BigDecimal total;

    private String note;
    private Long createdBy;

    private List<PurchaseOrderItemResponse> items;
}
