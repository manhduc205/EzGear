package com.manhduc205.ezgear.dtos.responses;


import com.manhduc205.ezgear.enums.TransferStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StockTransferResponse {
    private Long id;
    private String code;
    private String fromWarehouseName;
    private String toWarehouseName;
    private String createdByName;
    private TransferStatus status;
    private String note;
    private LocalDateTime createdAt;
    private List<TransferItemResponse> items;

    @Data
    @Builder
    public static class TransferItemResponse {
        private Long skuId;
        private String skuCode;
        private String productName;
        private String imageUrl;
        private Integer quantity;
    }
}