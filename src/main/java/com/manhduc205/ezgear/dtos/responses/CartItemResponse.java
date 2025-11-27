package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponse {
    private Long skuId;
    private String skuName;
    private String productName;
    private String imageUrl;
    private Long price;
    private int quantity;
    private boolean selected;
    private boolean isOutOfStock; // True nếu hết hàng
    private int availableQuantity; // Số lượng còn lại tại khu vực đó
}
