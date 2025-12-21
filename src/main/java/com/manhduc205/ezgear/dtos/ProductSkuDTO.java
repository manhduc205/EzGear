package com.manhduc205.ezgear.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSkuDTO {
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotBlank(message = "SKU is required")
    private String sku;

    private String name;

    private String optionName;
    private String skuImage;
    private Long price;

    private String barcode;
    private Integer weightGram;
    private Integer lengthCm;
    private Integer widthCm;
    private Integer heightCm;
    private Boolean isActive = true;
}
