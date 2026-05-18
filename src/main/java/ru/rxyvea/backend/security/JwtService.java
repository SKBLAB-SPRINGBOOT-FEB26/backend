package ru.rxyvea.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
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

    public void applyTokensCookies(
            @NonNull HttpServletResponse response,
            String accessToken,
            String refreshToken
    ) {
        final var accessTokenCookie = new Cookie("access_token", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setMaxAge(Math.toIntExact(jwtProperties.getExpiry()));
        accessTokenCookie.setPath("/");

        final var refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setMaxAge(Math.toIntExact(jwtProperties.getRefreshExpiry()));
        refreshTokenCookie.setPath("/");

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }
}
