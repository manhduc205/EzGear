package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CartCheckoutPreviewResponse {
    private List<CheckoutItemResponse> items;
    private Long subtotal;
    private Long discount; // giảm giá tạm thời từ voucher
    private Long total;
}
