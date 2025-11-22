package com.manhduc205.ezgear.dtos.responses.voucher;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplyVoucherResponse {
    private String code;
    private Long discount;
}

