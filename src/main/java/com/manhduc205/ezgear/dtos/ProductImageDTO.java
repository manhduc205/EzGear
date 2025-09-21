package com.manhduc205.ezgear.dtos;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ProductImageDTO {
    @JsonProperty("product_id")
    @Min(value = 1)
    private Long productId;

    @Size(min = 5, max = 300)
    @JsonProperty("image_url")
    private String imageUrl;
}
