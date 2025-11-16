package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CartResponse {
    private Long userId;
    private List<CartItemResponse> items;
}
