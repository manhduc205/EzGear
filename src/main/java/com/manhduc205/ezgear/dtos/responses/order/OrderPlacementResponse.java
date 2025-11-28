package com.manhduc205.ezgear.dtos.responses.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderPlacementResponse {
    private Long orderId;
    private String orderCode;
    private String status;       // WAITING_PAYMENT hoặc PENDING_SHIPMENT
    private String paymentUrl;   // Null nếu là COD, Có link nếu là VNPay
    private String message;
}
