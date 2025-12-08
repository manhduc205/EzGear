package com.manhduc205.ezgear.security;

import com.manhduc205.ezgear.models.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final Set<? extends GrantedAuthority> authorities;
    private final Long branchId;

    public CustomUserDetails(User user, Set<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;

        //  user có branch thì lấy ID, nếu không (SysAdmin/Khách) thì null
        if (user.getBranch() != null) {
            this.branchId = user.getBranch().getId();
        } else {
            this.branchId = null;
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash() != null ? user.getPasswordHash() : "";
    }

    @Override
    public String getUsername() {
        return user.getEmail();
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

    public Long getId() {
        return user.getId();
    }

}