package com.example.user_service.exception;

public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }

    public ApiException() {
        super("An error occurred");
    }
}

