package com.manhduc205.ezgear.dtos.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String message;
    private boolean success = false;
    private T payload;
    private List<String> errors;
    private String error;
    private Long id;
}
