package com.manhduc205.ezgear.model;
import com.manhduc205.ezgear.models.User;
import com.manhduc205.ezgear.models.Role;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Column(name = "userroles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"}))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRole {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

}
