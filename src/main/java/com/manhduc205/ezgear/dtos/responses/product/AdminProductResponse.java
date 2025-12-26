package com.manhduc205.ezgear.dtos.responses.product;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminProductResponse {
    private Long id;
    private String name;
    private String seriesCode;
    private String imageUrl;
    private String categoryName;
    private String brandName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
