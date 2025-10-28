package com.manhduc205.ezgear.shipping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DistrictDTO {
    @JsonProperty("DistrictID")
    private Integer id;

    @JsonProperty("DistrictName")
    private String name;

    @JsonProperty("ProvinceID")
    private Integer provinceId;
}

