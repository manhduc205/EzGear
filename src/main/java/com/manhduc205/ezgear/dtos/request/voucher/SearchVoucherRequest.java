package com.manhduc205.ezgear.dtos.request.voucher;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchVoucherRequest {
    private String code;
    private String status;
    private String type;           // ORDER/SHIPPING
    private String scope;          // ALL/CATEGORY
    private String discountType;   // AMOUNT/PERCENT
    private Long minOrderFrom;
    private Long minOrderTo;
    private LocalDateTime startFrom;
    private LocalDateTime startTo;
}

