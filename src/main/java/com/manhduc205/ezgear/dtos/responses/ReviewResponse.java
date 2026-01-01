package com.manhduc205.ezgear.dtos.responses;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private String userName;
    private Integer rating;
    private String comment;
    private List<String> imageUrls;
    private String shopResponse;
    private LocalDateTime shopResponseAt;
    private LocalDateTime createdAt;
}