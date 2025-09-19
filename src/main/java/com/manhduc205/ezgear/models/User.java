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

    // mapping to userrole
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
//        List<SimpleGrantedAuthority> authoritiesList = new ArrayList<>();
//        authoritiesList.add(new SimpleGrantedAuthority("ROLE_" + getR.toUpperCase()));
//        return authoritiesList;
        return userRoles.stream().map(u -> new SimpleGrantedAuthority(u.getRole().getName().toUpperCase())).toList();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return "";
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
