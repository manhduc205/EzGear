package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CheckoutResponse {
    private OrderPreviewResponse orderPreview;
    private ShippingAddressInfo shippingAddress;
    private String paymentMethod;
    private VoucherInfo voucher;
    private WarehouseInfo warehouse;
    private String orderCode;
    private String paymentUrl;
    private String message;
}


