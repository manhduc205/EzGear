package com.manhduc205.ezgear.models.order;

import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {

    private List<CartItemRequest> cartItems;
    private Long addressId;
    private String voucherCode;
    private String paymentMethod;
    private String note;
    private BigDecimal shippingFee;
}
