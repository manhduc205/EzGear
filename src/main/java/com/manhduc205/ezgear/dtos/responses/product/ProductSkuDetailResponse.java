package com.manhduc205.ezgear.dtos.responses.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductSkuDetailResponse {
    private Long id;
    private String sku;
    private String skuName;
    private String optionName;
    private String skuImage;
    private Long price;
    private Boolean isStockAvailable;
}