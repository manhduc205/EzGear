package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class User extends AbstractEntity implements UserDetails {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true, length = 32)
    private String phone;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_account_id", unique = true)
    private String googleAccountId;

    @Column(name = "facebook_account_id", unique = true)
    private String facebookAccountId;

    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status = Status.ACTIVE;

    @Column(nullable = false)
    private Boolean isStaff = false;

    private java.time.LocalDateTime lastLoginAt;

    // mapping to userrole
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.EAGER)
    private Set<UserRole> userRoles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userRoles.stream()
                .map(u -> new SimpleGrantedAuthority("ROLE_" + u.getRole().getCode().toUpperCase()))
                .toList();
    }

    @ManyToOne(fetch = FetchType.EAGER) // Để EAGER lấy luôn thông tin branch khi query User
    @JoinColumn(name = "branch_id")
    private Branch branch;

    // Helper method để lấy ID nhanh (tránh null pointer)
    public Long getBranchId() {
        return branch != null ? branch.getId() : null;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }


    public enum Status {
        ACTIVE, INACTIVE, LOCKED
    }
}
