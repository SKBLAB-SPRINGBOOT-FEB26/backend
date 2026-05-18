package ru.rxyvea.backend.api.v1.auth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.rxyvea.backend.api.generated.AuthApi;
import ru.rxyvea.backend.api.generated.dto.LoginRequest;
import ru.rxyvea.backend.api.generated.dto.LoginResponse;
import ru.rxyvea.backend.api.generated.dto.SignupRequest;
import ru.rxyvea.backend.api.generated.dto.SuccessResponse;
import ru.rxyvea.backend.model.User;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final AuthService authService;

    @SneakyThrows
    @Override
    public ResponseEntity<LoginResponse> signup(SignupRequest signupRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.signup(signupRequest, currentResponse()));
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest, currentResponse()));
    }

    @Override
    public ResponseEntity<SuccessResponse> logout() {
        final var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        authService.logout(user, currentResponse());
        return ResponseEntity.ok(new SuccessResponse().ok(true));
    }

    private static HttpServletResponse currentResponse() {
        final var attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attrs.getResponse();
    }
}
