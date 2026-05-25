package ru.rxyvea.backend.api.v1.users;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import ru.rxyvea.backend.api.generated.UsersApi;
import ru.rxyvea.backend.api.generated.dto.KycStatus;
import ru.rxyvea.backend.api.generated.dto.UserResponse;
import ru.rxyvea.backend.model.User;
import ru.rxyvea.backend.repository.UserRepository;
import ru.rxyvea.backend.repository.projection.UserView;

@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<UserResponse> getMe() {
        final var principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final var view = userRepository.findViewWithRolesById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return ResponseEntity.ok(toResponse(view));
    }

    private static UserResponse toResponse(UserView v) {
        final var resp = new UserResponse(
                v.getId(),
                v.getEmail(),
                v.getFirstName(),
                v.getLastName(),
                KycStatus.valueOf(v.getKycStatus().name()),
                v.getRoles().stream().map(UserView.RoleView::getName).toList()
        );
        return resp;
    }
}
