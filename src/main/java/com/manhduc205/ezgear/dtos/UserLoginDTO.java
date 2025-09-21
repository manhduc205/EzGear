package com.manhduc205.ezgear.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data //toString
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginDTO {
    @JsonProperty("email")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password can not be blank")
    private String password;
}
