package com.manhduc205.ezgear.dtos.responses.voucher;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PromotionResponse {
    private Long id;
    private String code;
    private String type;
    private String discountType;
    private Long discountValue;
    private Long maxDiscount;
    private Long minOrder;
    private String scope;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private List<Long> applicableCategoryIds;
}
