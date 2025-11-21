package com.manhduc205.ezgear.shipping.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DistrictDTO {
    @JsonProperty("DistrictID")
    private Integer id;

    @JsonProperty("DistrictName")
    private String name;

    @JsonProperty("ProvinceID")
    private Integer provinceId;
}

