package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationOptionResponse {
    private Integer id;
    private String name;
}