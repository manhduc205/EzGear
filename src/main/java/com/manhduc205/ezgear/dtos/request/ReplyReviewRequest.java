package com.manhduc205.ezgear.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReplyReviewRequest {
    @NotBlank
    private String content;
}