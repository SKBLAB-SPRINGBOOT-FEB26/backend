package ru.rxyvea.backend.security.exceptions;

public class NotValidRefreshTokenException extends Exception {
    public NotValidRefreshTokenException() {
        super("Not valid refresh token provided");
    }
}