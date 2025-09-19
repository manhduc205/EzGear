package com.manhduc205.ezgear.conponents;
import com.manhduc205.ezgear.models.Token;
import com.manhduc205.ezgear.repositories.TokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenUtil {
    private final TokenRepository tokenRepository;
    // add minutes , ...
    @Value("${jwt.expiration}") // tính bằng giây
    private long expiration;

    @Value("${jwt.secret-key}") // secret key base64
    private String secretKey;

    // generate token using jwt utility class and return token as string
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        try {
            return Jwts
                    .builder()
                    .setClaims(extraClaims) // tùy biến payload
                    .setSubject(userDetails.getUsername())
                    .setIssuedAt(new Date(System.currentTimeMillis())) // iat thời điểm phát hành
                    .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                    .signWith(getSignInKey(),  SignatureAlgorithm.HS256) // ký
                    .compact();
        }catch (Exception e){
            log.error("Cannot genarate Token : {}",e.getMessage());
            return null;
        }
    }
    // decode and get the key
    private Key getSignInKey() {
        // decode SECRET_KEY
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    // Parse + get claims
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token).getBody();

    }
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    // extract user from token
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // if token expirated
    public boolean isTokenExpirated(String token) {
        return extractExpiration(token).before(new Date());
    }

    // get expiration data from token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    // Validate token
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        Token existingToken = tokenRepository.findByToken(token).orElse(null);

        if (existingToken == null || existingToken.isRevoked()) {
            return false;
        }
        return email.equals(userDetails.getUsername()) && !isTokenExpirated(token);
    }

}
