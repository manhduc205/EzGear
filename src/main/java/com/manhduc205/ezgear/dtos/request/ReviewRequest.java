package com.manhduc205.ezgear.dtos.request;

import lombok.Data;
import java.util.List;

@Data
public class ReviewRequest {
    private Long productId;
    private Long orderItemId;
    private Integer rating;
    private String comment;
    private List<String> imageUrls;
}