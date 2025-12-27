package com.manhduc205.ezgear.dtos.responses.product;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminProductDetailResponse {
    private Long id;
    private String name;
    private String slug;
    private String seriesCode;
    private String shortDesc;
    private String imageUrl;
    private Integer warrantyMonths;
    private Boolean isActive;
    private Long categoryId;
    private Long brandId;
    private String categoryName;
    private String brandName;
}

