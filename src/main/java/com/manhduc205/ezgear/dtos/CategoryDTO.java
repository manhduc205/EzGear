package com.manhduc205.ezgear.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
@Setter
public class CategoryDTO {
    @NotEmpty(message = "MessageKeys.CATEGORIES_NAME_REQUIRED")
    private String name;
    private String slug;

    @JsonProperty("parent_id")
    private Long parentId;

    @JsonProperty("brand_ids")
    private List<Long> brandIds;
}
