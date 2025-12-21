package com.manhduc205.ezgear.dtos.responses.product;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProductDetailResponse {
    private Long id;
    private String name;
    private String slug;
    private String seriesCode;
    private String shortDesc;
    private Double ratingAverage;
    private Integer reviewCount;
    // List biến thể
    private List<ProductSkuDetailResponse> skus;
    // Album ảnh slide
    private List<String> galleryImages;
}