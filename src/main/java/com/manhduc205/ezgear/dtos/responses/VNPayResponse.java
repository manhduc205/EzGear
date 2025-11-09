package com.manhduc205.ezgear.dtos.responses;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VNPayResponse {
    private String paymentUrl;
}
