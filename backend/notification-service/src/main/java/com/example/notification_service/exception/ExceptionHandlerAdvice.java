package com.example.notification_service.exception;

import com.example.notification_service.domain.Response;
import com.example.notification_service.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ExceptionHandlerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidationException(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage(),
                        (existingValue, newValue) -> existingValue
                ));
        return error(request, errors, "Provided arguments are not valid", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Response> handleConstraintViolationException(ConstraintViolationException ex,
                                                                       HttpServletRequest request) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (existingValue, newValue) -> existingValue
                ));
        return error(request, errors, "Provided arguments are not valid", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response> handleUnreadablePayload(HttpMessageNotReadableException ex,
                                                            HttpServletRequest request) {
        return error(request, Map.of(), "Request payload is not valid", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Response> handleDatabaseConstraintException(DataIntegrityViolationException ex,
                                                                      HttpServletRequest request) {
        return error(
                request,
                Map.of(),
                "Notification data violates persistence constraints",
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Response> handleNotificationNotFound(NotificationNotFoundException ex,
                                                               HttpServletRequest request) {
        return error(request, Map.of(), "Notification not found.", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Response> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        return error(request, Map.of(ex.getName(), "Value has an invalid format"),
                "Provided arguments are not valid", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Response> handleHandlerMethodValidation(HandlerMethodValidationException ex,
                                                                  HttpServletRequest request) {
        return error(request, Map.of(), "Provided arguments are not valid", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Response> handleIllegalArgument(IllegalArgumentException ex,
                                                          HttpServletRequest request) {
        return error(request, Map.of(), ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleGenericException(Exception ex, HttpServletRequest request) {
        return error(request, Map.of(), "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Response> error(HttpServletRequest request, Map<?, ?> data, String message,
                                           HttpStatus status) {
        return ResponseEntity.status(status)
                .body(RequestUtils.getResponse(request, data, message, status));
    }
}
