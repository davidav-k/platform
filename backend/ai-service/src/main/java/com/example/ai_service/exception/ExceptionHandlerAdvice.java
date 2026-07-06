package com.example.ai_service.exception;

import com.example.ai_service.domain.Response;
import com.example.ai_service.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
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

    @ExceptionHandler(AiProviderUnavailableException.class)
    public ResponseEntity<Response> handleProviderUnavailable(AiProviderUnavailableException ex,
                                                             HttpServletRequest request) {
        return error(request, Map.of(), "AI provider is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(AiProviderTimeoutException.class)
    public ResponseEntity<Response> handleProviderTimeout(AiProviderTimeoutException ex,
                                                         HttpServletRequest request) {
        return error(request, Map.of(), "AI provider request timed out", HttpStatus.GATEWAY_TIMEOUT);
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<Response> handleProviderFailure(AiProviderException ex,
                                                         HttpServletRequest request) {
        return error(request, Map.of(), "AI provider request failed", HttpStatus.BAD_GATEWAY);
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