package ru.rxyvea.backend.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rxyvea.backend.model.User;
import ru.rxyvea.backend.repository.projection.UserView;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesById(UUID id);

    @EntityGraph(attributePaths = "roles")
    Optional<UserView> findViewWithRolesByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<UserView> findViewWithRolesById(UUID id);
}
