package com.manhduc205.ezgear.shipping.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ProvinceDTO {
    @JsonProperty("ProvinceID")
    private Integer id;

    @JsonProperty("ProvinceName")
    private String name;

    @JsonProperty("Code")
    private String code;
}

