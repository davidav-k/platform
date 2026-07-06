package com.example.ai_service.exception;

public class AiProviderTimeoutException extends RuntimeException {

    public AiProviderTimeoutException(String message) {
        super(message);
    }

    public AiProviderTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}