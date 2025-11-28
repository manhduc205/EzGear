package com.manhduc205.ezgear.dtos.request.order;

import lombok.*;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    private List<CartItemRequest> cartItems;
    private Long addressId;
    private Long branchId;
    private String note;
    private String voucherCode; // optional
    private String paymentMethod;
    private Integer shippingServiceId;
}
