package com.manhduc205.ezgear.security;

import com.manhduc205.ezgear.components.JwtTokenUtil;
import com.manhduc205.ezgear.services.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwtToken = null;

        // B1: Lấy token từ header
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7);
            try {
                // B2: Validate token (đã check blacklist bên trong)
                if (jwtTokenUtil.validateToken(jwtToken)) {
                    username = jwtTokenUtil.getUsernameFromAccessToken(jwtToken);
                } else {
                    log.warn("JWT validation failed (maybe blacklisted)");
                }
            } catch (ExpiredJwtException e) {
                log.warn("JWT expired: {}", e.getMessage());
            } catch (Exception e) {
                log.warn("JWT invalid: {}", e.getMessage());
            }
        }

        // B3: Nếu token hợp lệ, set Authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtTokenUtil.validateToken(jwtToken)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("===========================================");
                System.out.println(">>> [JwtAuthenticationFilter] Authenticated user: " + username);
                System.out.println(">>> [JwtAuthenticationFilter] Authorities: " + authToken.getAuthorities());
                System.out.println(">>> [JwtAuthenticationFilter] Token valid: " + jwtTokenUtil.validateToken(jwtToken));
                System.out.println("===========================================");
            }
        }

        // B4: Tiếp tục chuỗi filter
        filterChain.doFilter(request, response);
    }
}
