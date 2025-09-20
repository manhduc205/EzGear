package com.manhduc205.ezgear.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BrandDTO {
    private Long id;
    @NotBlank(message = "Brand name is required")
    private String name;

    private String slug;

}
