package com.manhduc205.ezgear.shipping.dto.request;

import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import lombok.Data;

import java.util.List;

@Data
public class ShippingFeeRequest {
    private Long branchId;
    private Long addressId;
    private List<CartItemRequest> cartItems;
    private Integer serviceId;
}
