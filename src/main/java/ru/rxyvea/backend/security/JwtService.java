package ru.rxyvea.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import ru.rxyvea.backend.model.User;
import ru.rxyvea.backend.security.props.JwtProperties;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;
    private final ECPublicKey jwtPublicKey;
    private final ECPrivateKey jwtPrivateKey;

    private JwtBuilder buildToken(User principal) {
        return Jwts.builder()
                .subject(String.valueOf(principal.getId()))
                .issuedAt(new Date(System.currentTimeMillis()))
                .encryptWith(jwtPublicKey, Jwts.KEY.ECDH_ES_A256KW, Jwts.ENC.A256GCM);
    }

    public String issueAccessToken(User principal) {
        return buildToken(principal)
                .claim("typ", "access")
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiry() * 1000))
                .compact();
    }

    public String issueRefreshToken(User principal) {
        return buildToken(principal)
                .claim("typ", "refresh")
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshExpiry() * 1000))
                .compact();
    }

    public Claims validateToken(String token) throws io.jsonwebtoken.JwtException, IllegalArgumentException {
        return Jwts.parser()
                .decryptWith(jwtPrivateKey)
                .build()
                .parseEncryptedClaims(token)
                .getPayload();
    }

    public ResponseCookie buildAccessTokenCookie(String accessToken) {
        return buildCookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME, accessToken, jwtProperties.getExpiry());
    }

    public ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        return buildCookie(JwtAuthenticationFilter.REFRESH_TOKEN_COOKIE_NAME, refreshToken, jwtProperties.getRefreshExpiry());
    }

    public ResponseCookie buildClearCookie(String name) {
        return buildCookie(name, "", 0L);
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
