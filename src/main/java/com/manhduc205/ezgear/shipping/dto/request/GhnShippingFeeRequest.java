package com.manhduc205.ezgear.shipping.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GhnShippingFeeRequest {
    @JsonProperty("service_id")
    private Integer serviceId;

    @JsonProperty("insurance_value")
    private Integer insuranceValue; // Giá trị hàng hoá (VNĐ)

    @JsonProperty("from_district_id")
    private Integer fromDistrictId;

    @JsonProperty("to_district_id")
    private Integer toDistrictId;

    @JsonProperty("to_ward_code")
    private String toWardCode;

    @JsonProperty("height")
    private Integer height; // cm

    @JsonProperty("length")
    private Integer length; // cm

    @JsonProperty("weight")
    private Integer weight; // gram

    @JsonProperty("width")
    private Integer width; // cm

    @JsonProperty("shop_id")
    private Integer shopId;
}
