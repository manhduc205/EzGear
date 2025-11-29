package com.manhduc205.ezgear.shipping.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GhnCreateOrderResponse {

    private int code; // 200 là thành công
    private String message;
    private Data data;

    @lombok.Data
    @NoArgsConstructor
    public static class Data {

        @JsonProperty("order_code")
        private String orderCode; // Mã vận đơn

        @JsonProperty("total_fee")
        private Long totalFee; // Tổng cước phí thực tế

        @JsonProperty("expected_delivery_time")
        private String expectedDeliveryTime; // Thời gian giao dự kiến
    }
}