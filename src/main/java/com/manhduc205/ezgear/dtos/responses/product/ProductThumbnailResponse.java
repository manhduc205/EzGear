package com.manhduc205.ezgear.dtos.responses.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductThumbnailResponse {
    private Long id;
    private String skuCode;

    private String name;
    private String slug;

    private Long price;

    private String imageUrl;

    private Boolean isStockAvailable;
}