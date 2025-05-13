package com.example.user_service.exception;

import com.example.user_service.domain.Response;
import com.example.user_service.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ExceptionHandlerAdvice {

    @ExceptionHandler(ApiException.class)
    public Response handleApiException(ApiException ex, HttpServletRequest request) {
        return RequestUtils.getResponse(request, Map.of(), ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage(),
                        (existingValue, newValue) -> existingValue));
        return RequestUtils.getResponse(request, errors, "Provided arguments are not valid", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Response handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage()));
        return RequestUtils.getResponse(request, errors, "Constraint violation", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Response handleDatabaseConstraintException(DataIntegrityViolationException ex, HttpServletRequest request) {
        return RequestUtils.getResponse(request, Map.of(), "User with this email already exists", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Response handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        return RequestUtils.getResponse(request, Map.of(), "Access denied: " + ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public Response handleGenericException(Exception ex, HttpServletRequest request) {
        return RequestUtils.getResponse(request, Map.of("error", ex.getMessage()), "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
