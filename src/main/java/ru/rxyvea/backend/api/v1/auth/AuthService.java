package ru.rxyvea.backend.api.v1.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rxyvea.backend.api.generated.dto.LoginRequest;
import ru.rxyvea.backend.api.generated.dto.LoginResponse;
import ru.rxyvea.backend.api.generated.dto.SignupRequest;
import ru.rxyvea.backend.model.KycStatus;
import ru.rxyvea.backend.model.Role;
import ru.rxyvea.backend.model.User;
import ru.rxyvea.backend.repository.RoleRepository;
import ru.rxyvea.backend.repository.UserRepository;
import ru.rxyvea.backend.security.JwtAuthenticationFilter;
import ru.rxyvea.backend.security.JwtService;
import ru.rxyvea.backend.security.RefreshService;
import ru.rxyvea.backend.service.exceptions.UserAlreadyExistsWithFieldException;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshService refreshService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse signup(SignupRequest request, HttpServletResponse response)
            throws UserAlreadyExistsWithFieldException {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsWithFieldException("email");
        }

        final var roleUser = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not seeded — apply Liquibase migrations"));

        final var user = User.builder()
                .email(request.getEmail())
                .passwdHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .kycStatus(KycStatus.PENDING)
                .enabled(true)
                .locked(false)
                .roles(Set.of(roleUser))
                .build();

        final var saved = userRepository.save(user);
        return issueTokens(saved, response);
    }

    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        final var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        return issueTokens((User) authentication.getPrincipal(), response);
    }

    private LoginResponse issueTokens(User user, HttpServletResponse response) {
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
