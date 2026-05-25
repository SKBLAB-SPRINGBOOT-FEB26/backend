package ru.rxyvea.backend.api.v1.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
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

import java.util.List;
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

    /**
     * Result of an auth flow: the JSON body and the {@code Set-Cookie} headers the
     * controller must attach to its {@link org.springframework.http.ResponseEntity}.
     */
    public record AuthResult(LoginResponse body, List<ResponseCookie> cookies) {
    }

    @Transactional
    public AuthResult signup(SignupRequest request)
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
        return issueTokens(saved);
    }

    public AuthResult login(LoginRequest request) {
        final var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        return issueTokens((User) authentication.getPrincipal());
    }

    private AuthResult issueTokens(User user) {
        final var accessToken = jwtService.issueAccessToken(user);
        final var refreshToken = jwtService.issueRefreshToken(user);

        refreshService.storeRefreshToken(user.getId(), refreshToken);

        final var body = new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).toList()
        );

        return new AuthResult(body, List.of(
                jwtService.buildAccessTokenCookie(accessToken),
                jwtService.buildRefreshTokenCookie(refreshToken)
        ));
    }

    public List<ResponseCookie> logout(User user) {
        refreshService.revokeRefreshToken(user.getId());
        return List.of(
                jwtService.buildClearCookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME),
                jwtService.buildClearCookie(JwtAuthenticationFilter.REFRESH_TOKEN_COOKIE_NAME)
        );
    }
}
