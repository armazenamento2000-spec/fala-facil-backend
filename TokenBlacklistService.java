package com.falafacil.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void addToBlacklist(String token) {
        long expirationTime = extractExpirationTime(token);
        long ttl = expirationTime - System.currentTimeMillis();

        if (ttl > 0) {
            redisTemplate.opsForValue().set("blacklist:" + token, "true", ttl, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }

    private long extractExpirationTime(String token) {
        // Aqui você extrairia do JWT a data de expiração
        // Por simplicidade, usamos um padrão de 15 minutos
        return System.currentTimeMillis() + 900000; // 15 minutos
    }
}
