package com.manhduc205.ezgear.dtos.responses;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private String orderCode;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shippingFee;
    private BigDecimal total;
    private String paymentMethod;
    private String paymentUrl;
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
}
