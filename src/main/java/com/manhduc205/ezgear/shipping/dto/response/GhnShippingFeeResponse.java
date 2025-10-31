package com.manhduc205.ezgear.shipping.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response từ GHN khi gọi API /v2/shipping-order/fee
 * https://api.ghn.vn/home/docs/detail?id=76
 */
@Data
public class GhnShippingFeeResponse {

    private int code;
    private DataResponse data;
    private String message;

    @Data
    public static class DataResponse {

        @JsonProperty("total")
        private Integer total; // Tổng phí ship (đã gồm phụ phí)

        @JsonProperty("service_fee")
        private Integer serviceFee; // Phí dịch vụ cơ bản

        @JsonProperty("insurance_fee")
        private Integer insuranceFee; // Phí bảo hiểm hàng hóa

        @JsonProperty("pick_station_fee")
        private Integer pickStationFee; // Phí lấy hàng (nếu có)

        @JsonProperty("coupon_value")
        private Integer couponValue; // Giảm giá (nếu có)

        @JsonProperty("r2s_fee")
        private Integer r2sFee; // Phí giao lại (nếu có)

        @JsonProperty("document_return")
        private Integer documentReturn; // Phí trả chứng từ

        @JsonProperty("double_check")
        private Integer doubleCheck; // Phí kiểm tra hàng 2 lần

        @JsonProperty("cod_fee")
        private Integer codFee; // Phí thu hộ COD

        @JsonProperty("delivery_type")
        private String deliveryType; // Loại hình giao (door to door, station...)
    }
}
