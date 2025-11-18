package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CheckoutItemPreviewResponse {
    private Long skuId;
    private String productName;
    private String skuName;
    private String imageUrl;
    private Long price;
    private Integer quantity;
    private Long lineTotal;
    private Boolean selected;
}

