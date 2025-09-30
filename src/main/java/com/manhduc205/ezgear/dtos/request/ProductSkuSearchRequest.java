package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSkuSearchRequest {
    private String sku;
    private String name;        // tên SKU
    private String productName; // tên sản phẩm cha
    private String brandName;
    private String categoryName;
    private String barcode;
    private Boolean isActive;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private Integer page = 0;
    private Integer size = 20;
}
