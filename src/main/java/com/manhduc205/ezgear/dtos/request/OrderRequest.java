package com.manhduc205.ezgear.dtos.request;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {

    private Long userId;

    private Long shippingAddressId;

    private Long subtotal;

    private Long discountTotal;

    private Long shippingFee;

    private Long grandTotal;

    private String paymentMethod;

    private List<OrderItemRequest> items;
}
