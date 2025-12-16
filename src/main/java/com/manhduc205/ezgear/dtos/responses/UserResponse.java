package com.manhduc205.ezgear.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.manhduc205.ezgear.models.User;
import lombok.*;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;

    @JsonProperty("full_name")
    private String fullName;

    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;

    private String status;

    @JsonProperty("is_staff")
    private boolean staff;

    @JsonProperty("roles")
    private Set<String> roles;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhone())
                .status(user.getStatus().name())
                .staff(Boolean.TRUE.equals(user.getIsStaff()))
                .roles(user.getUserRoles().stream()
                        .map(ur -> ur.getRole().getCode())
                        .collect(Collectors.toSet()))
                .build();
    }
}
