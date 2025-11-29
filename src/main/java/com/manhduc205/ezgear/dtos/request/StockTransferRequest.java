package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

import java.util.List;

@Data
public class StockTransferRequest {
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private String note;
    private List<TransferItemRequest> items;

    @Data
    public static class TransferItemRequest {
        private Long skuId;
        private Integer quantity;
    }
}

