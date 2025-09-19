package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class User extends AbstractEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(unique = true, length = 32)
    private String phone;

    @Column(nullable = false, name = "password_hash")
    private String passwordHash;

    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status = Status.ACTIVE;

    @Column(nullable = false)
    private Boolean isStaff = false;

    private java.time.LocalDateTime lastLoginAt;


    public enum Status {
        ACTIVE, INACTIVE, LOCKED
    }
}
