package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

@Data
public class AdminProductSearchRequest {
    private String keyword;
    private Long categoryId;
    private Long brandId;
    private Boolean isActive;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDir;
}
