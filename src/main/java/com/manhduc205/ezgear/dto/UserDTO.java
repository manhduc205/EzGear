package com.manhduc205.ezgear.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    @JsonProperty("full_name")
    @NotBlank(message = "")
    private String fullName;

    @JsonProperty("email")
    @NotBlank(message = "")
    private String email;

    @JsonProperty("phone_number")
    @NotBlank(message = "")
    private String phoneNumber;

    @JsonProperty("password")
    @NotBlank(message = "")
    private String password;

    @JsonProperty("retype_password")
    @NotBlank(message = "MessageKeys.RETYPE_PASSWORD_REQUIRED")
    private String retypePassword;

    @NotNull(message = "Role id is required")
    @JsonProperty("role_id")
    private Long roleId;
}
