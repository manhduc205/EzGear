package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class VoucherInfo {
    private String code;
    private Long discountValue;
}

