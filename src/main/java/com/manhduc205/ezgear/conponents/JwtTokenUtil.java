package com.manhduc205.ezgear.conponents;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenUtil {


    @Value("${jwt.accessKey}")
    private String accessKeyBase64;

    @Value("${jwt.refreshKey}")
    private String refreshKeyBase64;

    @Value("${jwt.expiryMinutes}") // AccessToken (phút)
    private long expiryMinutes;

    @Value("${jwt.expiryDay}")     // RefreshToken (ngày)
    private long expiryDay;

    private Key accessKey;
    private Key refreshKey;

    @PostConstruct
    public void init() {
        accessKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(accessKeyBase64));
        refreshKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(refreshKeyBase64));
    }

    // ================= ACCESS TOKEN =================

    public String generateAccessToken(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return buildToken(principal.getUsername(), roles, accessKey, getAccessTokenExpiryDate());
    }

//    public boolean validateAccessToken(String token, UserDetails userDetails) {
//        if (!validateToken(token, accessKey)) return false;
//
//        String username = getUsernameFromAccessToken(token);
//        Token existingToken = tokenRepository.findByToken(token).orElse(null);
//        if (existingToken == null || existingToken.isRevoked()) {
//            return false;
//        }
//
//        return username.equals(userDetails.getUsername());
//    }

//    public String getUsernameFromAccessToken(String token) {
//        return extractClaims(token, accessKey).getSubject();
//    }
//
//    public String getRolesFromAccessToken(String token) {
//        return extractClaims(token, accessKey).get("roles", String.class);
//    }
//
//    public LocalDateTime getAccessTokenExpiry(String token) {
//        return toLocalDateTime(extractClaims(token, accessKey).getExpiration());
//    }

    // ================= REFRESH TOKEN =================

    public String generateRefreshToken(String username) {
        return buildToken(username, null, refreshKey, getRefreshTokenExpiryDate());
    }

//    public boolean validateRefreshToken(String token) {
//        if (!validateToken(token, refreshKey)) return false;
//
//        Token existingToken = tokenRepository.findByToken(token).orElse(null);
//        return existingToken != null && !existingToken.isRevoked();
//    }

    public String getUsernameFromRefreshToken(String token) {
        return extractClaims(token, refreshKey).getSubject();
    }

    // ================= COMMON UTILS =================

    private String buildToken(String subject, String roles, Key key, Date expiryDate) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512);

        if (roles != null) {
            builder.claim("roles", roles);
        }

        return builder.compact();
    }

//    private boolean validateToken(String token, Key key) {
//        try {
//            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
//            return true;
//        } catch (JwtException | IllegalArgumentException ex) {
//            log.error("Invalid JWT token: {}", ex.getMessage());
//            return false;
//        }
//    }

    private Claims extractClaims(String token, Key key) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Date getAccessTokenExpiryDate() {
        return new Date(System.currentTimeMillis() + 1000 * 60 * expiryMinutes);
    }

    private Date getRefreshTokenExpiryDate() {
        return new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * expiryDay);
    }

//    private LocalDateTime toLocalDateTime(Date date) {
//        return date.toInstant()
//                .atZone(java.time.ZoneId.systemDefault())
//                .toLocalDateTime();
//    }
    // TTL của refreshToken (ms) → dùng cho Redis
    public long getRefreshTokenExpiryDuration() {
        return 1000L * 60 * 24 * expiryDay;
    }
}
