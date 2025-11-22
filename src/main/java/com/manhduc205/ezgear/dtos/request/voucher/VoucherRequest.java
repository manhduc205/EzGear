package com.manhduc205.ezgear.dtos.request.voucher;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VoucherRequest {
    private String code;            // chỉ dùng khi create, update có thể bỏ qua
    private String type;            // ORDER / SHIPPING
    private String discountType;    // AMOUNT / PERCENT
    private Long discountValue;
    private Long minOrder;
    private Long maxDiscount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer usageLimit;
    private String scope;           // ALL / CATEGORY
    private String status;          // ACTIVE / INACTIVE
    private List<Long> categoryIds;
}

