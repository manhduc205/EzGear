package com.manhduc205.ezgear.dtos;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDTO {
    private Long id;
    private String code;
    private String supplierName;
    private Long warehouseId;
    private String status;
    private Long  subtotal;
    private Long  total;
    private String note;
    private Long createdBy;
    private List<PurchaseOrderItemDTO> items;
}
