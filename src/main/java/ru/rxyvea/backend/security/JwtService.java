package ru.rxyvea.backend.security;

import ru.rxyvea.backend.model.User;
import ru.rxyvea.backend.security.props.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;

    private ECPublicKey publicKey;
    private ECPrivateKey privateKey;

    @PostConstruct
    public void initKeys() throws IOException {
        this.publicKey = jwtProperties.publicKey();
        this.privateKey = jwtProperties.privateKey();
    }

    private JwtBuilder buildToken(User principal) {
        return Jwts.builder()
                .subject(String.valueOf(principal.getId()))
                .issuedAt(new Date(System.currentTimeMillis()))
                .encryptWith(publicKey, Jwts.KEY.ECDH_ES_A256KW, Jwts.ENC.A256GCM);
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
                .decryptWith(privateKey)
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
//        accessTokenCookie.setSecure(true);
        accessTokenCookie.setMaxAge(Math.toIntExact(jwtProperties.getExpiry()));
        accessTokenCookie.setPath("/");

        final var refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
//        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setMaxAge(Math.toIntExact(jwtProperties.getRefreshExpiry()));
        refreshTokenCookie.setPath("/");

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }
}
