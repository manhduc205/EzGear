package com.manhduc205.ezgear.dtos.responses.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductSiblingResponse {
    private Long id;
    private String name;
    private String slug;
    private String imageUrl;
    private Long price;
    private Boolean isCurrent;
}