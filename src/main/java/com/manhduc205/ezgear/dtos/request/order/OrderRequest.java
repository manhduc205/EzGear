package com.manhduc205.ezgear.dtos.request.order;

import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {

    private Long branchId;
    private Long addressId;
    private Long shippingMethodId; // Service ID của GHN/GHTK
    private String voucherCode;
    private String note;
    private String paymentMethod; // "COD" hoặc "VNPAY"
    private List<CartItemRequest> items; // Chỉ chứa skuId và quantity
}
