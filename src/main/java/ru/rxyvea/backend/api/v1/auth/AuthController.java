package ru.rxyvea.backend.api.v1.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rxyvea.backend.api.v1.auth.dto.LoginRequest;
import ru.rxyvea.backend.api.v1.auth.dto.LoginResponse;
import ru.rxyvea.backend.api.v1.model.SuccessResponse;
import ru.rxyvea.backend.model.User;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.login(request, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse> logout(
            @AuthenticationPrincipal User user,
            HttpServletResponse response
    ) {
        authService.logout(user, response);
        return ResponseEntity.ok(SuccessResponse.builder().build());
    }
}
