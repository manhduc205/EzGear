package com.manhduc205.ezgear.dtos;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
@Setter
public class CategoryDTO {
    @NotEmpty(message = "MessageKeys.CATEGORIES_NAME_REQUIRED")
    private String name;
}
