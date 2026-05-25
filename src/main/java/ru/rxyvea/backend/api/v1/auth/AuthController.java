package ru.rxyvea.backend.api.v1.auth;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import ru.rxyvea.backend.api.generated.AuthApi;
import ru.rxyvea.backend.api.generated.dto.LoginRequest;
import ru.rxyvea.backend.api.generated.dto.LoginResponse;
import ru.rxyvea.backend.api.generated.dto.SignupRequest;
import ru.rxyvea.backend.api.generated.dto.SuccessResponse;
import ru.rxyvea.backend.model.User;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final AuthService authService;

    @SneakyThrows
    @Override
    public ResponseEntity<LoginResponse> signup(SignupRequest signupRequest) {
        final var result = authService.signup(signupRequest);
        return withCookies(ResponseEntity.status(HttpStatus.CREATED), result.cookies())
                .body(result.body());
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        final var result = authService.login(loginRequest);
        return withCookies(ResponseEntity.ok(), result.cookies())
                .body(result.body());
    }

    @Override
    public ResponseEntity<SuccessResponse> logout() {
        final var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final var cookies = authService.logout(user);
        return withCookies(ResponseEntity.ok(), cookies)
                .body(new SuccessResponse().ok(true));
    }

    private static ResponseEntity.BodyBuilder withCookies(
            ResponseEntity.BodyBuilder builder,
            List<ResponseCookie> cookies
    ) {
        cookies.forEach(cookie -> builder.header(HttpHeaders.SET_COOKIE, cookie.toString()));
        return builder;
    }
}
