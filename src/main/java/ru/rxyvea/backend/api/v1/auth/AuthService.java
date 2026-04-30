package ru.rxyvea.backend.api.v1.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import ru.rxyvea.backend.api.v1.auth.dto.LoginRequest;
import ru.rxyvea.backend.api.v1.auth.dto.LoginResponse;
import ru.rxyvea.backend.model.Role;
import ru.rxyvea.backend.model.User;
import ru.rxyvea.backend.security.JwtAuthenticationFilter;
import ru.rxyvea.backend.security.JwtService;
import ru.rxyvea.backend.security.RefreshService;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshService refreshService;

    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        final var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        final var user = (User) authentication.getPrincipal();

        final var accessToken = jwtService.issueAccessToken(user);
        final var refreshToken = jwtService.issueRefreshToken(user);

        refreshService.storeRefreshToken(user.getId(), refreshToken);
        jwtService.applyTokensCookies(response, accessToken, refreshToken);

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).toList()
        );
    }

    public void logout(User user, HttpServletResponse response) {
        refreshService.revokeRefreshToken(user.getId());
        clearTokenCookie(response, JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME);
        clearTokenCookie(response, JwtAuthenticationFilter.REFRESH_TOKEN_COOKIE_NAME);
    }

    private void clearTokenCookie(HttpServletResponse response, String name) {
        final var cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
