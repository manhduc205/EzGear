package com.manhduc205.ezgear.security;


import com.manhduc205.ezgear.models.User;
import com.manhduc205.ezgear.models.UserRole;
import com.manhduc205.ezgear.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + email));
        Set<GrantedAuthority> authorities = user.getUserRoles() != null ?
                user.getUserRoles().stream()
                        .map(UserRole::getRole)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode().toUpperCase()))
                        .collect(Collectors.toSet()) : Set.of();
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new CustomUserDetails(user, authorities);
    }
}
