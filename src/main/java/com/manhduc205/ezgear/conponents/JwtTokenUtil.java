package com.manhduc205.ezgear.conponents;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.BlacklistService;
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
    private final BlacklistService blacklistService;

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

        Long branchId = null;
        if (principal instanceof CustomUserDetails) {
            branchId = ((CustomUserDetails) principal).getBranchId();
        }

        // [MODIFIED] Truyền thêm branchId vào hàm buildToken
        return buildToken(principal.getUsername(), roles, branchId, accessKey, getAccessTokenExpiryDate());
    }

    // ================= REFRESH TOKEN =================

    public String generateRefreshToken(String username) {
        // Refresh token không cần lưu branchId (để nó chung chung) -> Truyền null
        return buildToken(username, null, null, refreshKey, getRefreshTokenExpiryDate());
    }

    public boolean validateToken(String token) {
        try {
            if (blacklistService.isBlacklisted(token)) {
                log.warn("Token is blacklisted");
                return false;
            }
            Jwts.parserBuilder().setSigningKey(accessKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getUsernameFromRefreshToken(String token) {
        return extractClaims(token, refreshKey).getSubject();
    }
    public String getUsernameFromAccessToken(String token) {
        return extractClaims(token, accessKey).getSubject();
    }
    public Long getBranchIdFromToken(String token) {
        Claims claims = extractClaims(token, accessKey);
        return claims.get("branchId", Long.class);
    }


    // ================= COMMON UTILS =================

    private String buildToken(String subject, String roles, Long branchId, Key key, Date expiryDate) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512);

        if (roles != null) {
            builder.claim("roles", roles);
        }

        if (branchId != null) {
            builder.claim("branchId", branchId);
        }

        return builder.compact();
    }

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

    // TTL của refreshToken (ms) → dùng cho Redis
    public long getRefreshTokenExpiryDuration() {
        return 1000L * 60 * 60 * 24 * expiryDay;
    }
    public Date getAccessTokenExpiry(String accesstoken) {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(accessKey)
                    .build()
                    .parseClaimsJws(accesstoken)
                    .getBody();
            return claims.getExpiration();

    }

}
