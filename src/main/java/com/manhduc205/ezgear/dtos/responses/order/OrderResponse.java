package com.manhduc205.ezgear.dtos.responses.order;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private Long id;
    private String orderCode;
    private String status;
    private String paymentStatus;
    private String paymentMethod;
    private LocalDateTime createdAt;


    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private List<OrderItemResponse> items;

    private Long merchandiseSubtotal;
    private Long shippingFee;
    private Long voucherDiscount;
    private Long grandTotal;

    @Data
    @Builder
    public static class OrderItemResponse {
        private Long productId;
        private String productName;
        private String skuName;
        private String imageUrl;
        private Integer quantity;
        private Long originalPrice;
        private Long price;
    }
}
