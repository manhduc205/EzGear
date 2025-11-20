package com.manhduc205.ezgear.shipping.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class WardDTO {
    @JsonProperty("WardCode")
    private String code;

    @JsonProperty("WardName")
    private String name;

    @JsonProperty("DistrictID")
    private Integer districtId;
}

