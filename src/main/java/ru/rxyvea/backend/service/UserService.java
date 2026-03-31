package ru.rxyvea.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.rxyvea.backend.model.User;
import ru.rxyvea.backend.repository.UserRepository;
import ru.rxyvea.backend.service.exceptions.UserAlreadyExistsWithFieldException;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;

    public UserDetailsService userDetailsService() {
        return email -> repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    public User create(User user) throws UserAlreadyExistsWithFieldException {
        if (repository.findByEmail(user.getEmail()).isPresent()) {
            throw new UserAlreadyExistsWithFieldException("email");
        }

        return repository.save(user);
    }
}
