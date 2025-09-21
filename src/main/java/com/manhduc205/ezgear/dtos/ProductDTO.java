package com.manhduc205.ezgear.dtos;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDTO {
    private Long id;

    private Long categoryId;
    private Long brandId;

    private String name;
    private String slug;
    private String shortDesc;
    private String imageUrl;
    private Integer warrantyMonths;
    private Boolean isActive;
}
