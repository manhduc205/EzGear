package com.manhduc205.ezgear.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;

    public void saveTokenRefresh(Long id, String refreshToken, long duration, TimeUnit timeUnit) {
        String key = "refresh_token:" + id;
        redisTemplate.opsForValue().set(key, refreshToken, duration, timeUnit);
    }

    public String getRefreshToken(Long userId) {

        return redisTemplate.opsForValue().get("refresh_token:"+ userId);
    }

    public void deleteTokenRefresh(Long userId) {

        redisTemplate.delete("refresh_token:"+ userId);
    }

    public void blacklistToken(String token, long expirationMillis) {
        redisTemplate.opsForValue().set(token, "revoked", expirationMillis, TimeUnit.MILLISECONDS);
    }
}
