package com.manhduc205.ezgear.dtos.responses;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private Long subtotal;
    private Long total;

    private String note;
    private Long createdBy;
    private LocalDateTime createdAt;
    private List<PurchaseOrderItemResponse> items;
}
