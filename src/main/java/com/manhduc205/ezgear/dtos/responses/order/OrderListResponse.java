package com.manhduc205.ezgear.dtos.responses.order;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderListResponse {
    private Long id;
    private String orderCode;
    private String status;          // Trạng thái đơn
    private String paymentStatus;   // Trạng thái thanh toán
    private String paymentMethod;   // Phương thức thanh toán
    private Long grandTotal;        // Tổng thanh toán (Số to nhất)
    private LocalDateTime createdAt;
    private List<OrderListItem> items;

    @Data
    @Builder
    public static class OrderListItem {
        private Long productId;
        private String productName;
        private String skuName;
        private String imageUrl;
        private Integer quantity;
        private Long price; // Giá bán tại thời điểm đó
    }
}