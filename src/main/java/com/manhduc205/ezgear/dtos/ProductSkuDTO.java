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

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be > 0")
    private BigDecimal price;

    private String barcode;
    private Integer weightGram;
    private String sizeMm;
    private Boolean isActive = true;
}
