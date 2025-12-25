package com.manhduc205.ezgear.dtos.responses.product;


import com.manhduc205.ezgear.models.AbstractEntity;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminProductSkuResponse {
    private Long id;
    private String skuCode;
    private String productName;
    private String skuName;
    private String categoryName;
    private String brandName;
    private String imageUrl;
    private Long price;
    private Integer warrantyMonths;
    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
