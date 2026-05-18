package ru.rxyvea.backend.repository.projection;

import ru.rxyvea.backend.model.KycStatus;

import java.util.Set;
import java.util.UUID;

public interface UserView {
    UUID getId();
    String getEmail();
    String getFirstName();
    String getLastName();
    KycStatus getKycStatus();
    Set<RoleView> getRoles();

    interface RoleView {
        Integer getId();
        String getName();
    }
}
