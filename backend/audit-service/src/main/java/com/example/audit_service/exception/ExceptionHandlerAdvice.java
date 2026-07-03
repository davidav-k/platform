package com.example.audit_service.exception;

import com.example.audit_service.domain.Response;
import com.example.audit_service.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice
public class ExceptionHandlerAdvice {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Response> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
                                                       HttpServletRequest request) {
        return error(
                request,
                Map.of(exception.getName(), "Value has an invalid format"),
                "Provided arguments are not valid",
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(AuditRecordNotFoundException.class)
    public ResponseEntity<Response> handleAuditRecordNotFound(AuditRecordNotFoundException exception,
                                                              HttpServletRequest request) {
        return error(request, Map.of(), "Audit record not found.", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Response> handleIllegalArgument(IllegalArgumentException exception,
                                                          HttpServletRequest request) {
        return error(request, Map.of(), exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleGenericException(Exception exception,
                                                           HttpServletRequest request) {
        return error(request, Map.of(), "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Response> error(HttpServletRequest request, Map<?, ?> data,
                                           String message, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(RequestUtils.getResponse(request, data, message, status));
    }
}
