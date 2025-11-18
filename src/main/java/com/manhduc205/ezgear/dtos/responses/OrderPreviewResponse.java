package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class OrderPreviewResponse {
    private List<CheckoutItemPreviewResponse> items;
    private Long subtotal;
    private Long discount;
    private Long shippingFee;
    private Long grandTotal;
}

