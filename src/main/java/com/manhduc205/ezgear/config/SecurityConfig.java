package com.manhduc205.ezgear.config;

import com.manhduc205.ezgear.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder);
        return builder.build();
    }
}
