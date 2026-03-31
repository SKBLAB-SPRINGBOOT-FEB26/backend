package ru.rxyvea.backend.service.exceptions;

import lombok.Getter;

@Getter
public class UserAlreadyExistsWithFieldException extends Exception {
    private final String field;

    public UserAlreadyExistsWithFieldException(String field) {
        super(String.format("A User with this '%s' already exists", field));
        this.field = field;
    }
}
