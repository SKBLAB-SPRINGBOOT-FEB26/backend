package ru.rxyvea.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rxyvea.backend.model.User;
import ru.rxyvea.backend.repository.projection.UserView;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<UserView> findViewByEmail(String email);

    Optional<UserView> findViewById(UUID id);
}
