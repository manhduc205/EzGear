package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.repositories.UserRepository;
import com.manhduc205.ezgear.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("customUserDetailsServiceV2")
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(user -> new CustomUserDetails(user, new java.util.HashSet<>(user.getAuthorities())))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
