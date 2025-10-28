package com.manhduc205.ezgear.shipping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ProvinceDTO {
    @JsonProperty("ProvinceID")
    private Integer id;

    @JsonProperty("ProvinceName")
    private String name;

    @JsonProperty("Code")
    private String code;
}

