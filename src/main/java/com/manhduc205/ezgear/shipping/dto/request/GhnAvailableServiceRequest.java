package com.manhduc205.ezgear.shipping.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GhnAvailableServiceRequest {
    @JsonProperty("from_district")
    private Integer fromDistrict;

    @JsonProperty("to_district")
    private Integer toDistrict;

    @JsonProperty("shop_id")
    private Integer shopId;
}
