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

    private BigDecimal subtotal;

    private BigDecimal discountTotal;

    private BigDecimal shippingFee;

    private BigDecimal grandTotal;

    private String paymentMethod;

    private List<OrderItemRequest> items;
}
