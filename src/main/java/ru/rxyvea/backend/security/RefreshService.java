package ru.rxyvea.backend.security;

import ru.rxyvea.backend.security.props.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshService {
    private static final String PREFIX = "refresh_token:";

    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, String> redisTemplate;

    public void storeRefreshToken(UUID id, String refreshToken) {
        final var key = PREFIX + id;
        redisTemplate.opsForValue().set(key, refreshToken);
        redisTemplate.expire(key, jwtProperties.getRefreshExpiry(), TimeUnit.SECONDS);
    }

    public boolean isActiveRefreshToken(UUID id, String providedToken) {
        final var storedToken = redisTemplate.opsForValue().get(PREFIX + id);
        return storedToken != null && storedToken.equals(providedToken);
    }

    public void revokeRefreshToken(UUID id) {
        redisTemplate.delete(PREFIX + id);
    }
}
