package com.example.api_gateway.exception;

import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiGatewayExceptionHandler {

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<String> handleJwtError(JwtException ex) {
        return ResponseEntity
                .status(401)
                .body("JWT error: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAll(Throwable ex) {
        return ResponseEntity
                .status(500)
                .body("Internal error: " + ex.getMessage());
    }
}
