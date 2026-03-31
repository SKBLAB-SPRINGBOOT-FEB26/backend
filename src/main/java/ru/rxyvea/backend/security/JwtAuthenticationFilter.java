package ru.rxyvea.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.rxyvea.backend.repository.UserRepository;
import ru.rxyvea.backend.security.exceptions.NotValidRefreshTokenException;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private final JwtService jwtService;
    private final RefreshService refreshService;

    private final UserRepository userRepository;

    private Optional<String> tryGetCookieValue(
            @NonNull HttpServletRequest request,
            @NonNull String name
    ) {
        return Optional.ofNullable(request.getCookies())
                .flatMap(x -> Arrays.stream(x)
                        .filter(y -> y.getName().equals(name))
                        .findFirst()
                        .map(Cookie::getValue)
                        .filter(z -> !z.isBlank())
                );
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        final var matchers = new PathPatternRequestMatcher[]{
                PathPatternRequestMatcher.withDefaults().matcher("/api/*/auth/login"),
                PathPatternRequestMatcher.withDefaults().matcher("/api/*/auth/signup"),
        };

        return Arrays.stream(matchers)
                .anyMatch(matcher -> matcher.matches(request));
    }

    @SneakyThrows
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) {
        final var accessToken = tryGetCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
        final var refreshToken = tryGetCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims;

            try {
                claims = jwtService.validateToken(accessToken.orElseThrow());
            } catch (Exception e) {
                if (refreshToken.isEmpty())
                    throw new AccessDeniedException("Missing both refresh & access token");

                claims = jwtService.validateToken(refreshToken.get());
            }

            final var subject = UUID.fromString(Objects.requireNonNull(claims.getSubject()));
            final var principal = userRepository.findById(subject)
                    .orElseThrow(() -> new AccessDeniedException("Insufficient rights"));

            if (Objects.equals(claims.get("typ", String.class), "refresh")) {
                if (!refreshService.isActiveRefreshToken(subject, refreshToken.orElseThrow())) {
                    throw new NotValidRefreshTokenException();
                }

                log.info("Issuing refresh for subject: {}", subject);

                final var newAccessToken = jwtService.issueAccessToken(principal);
                final var newRefreshToken = jwtService.issueRefreshToken(principal);

                refreshService.storeRefreshToken(principal.getId(), newRefreshToken);
                jwtService.applyTokensCookies(response, newAccessToken, newRefreshToken);
            }

            final var authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            );

            log.info("Issuing authentication for subject: {}", subject);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}