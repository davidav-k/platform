package com.example.user_service.utils;

import com.example.user_service.domain.Response;
import com.example.user_service.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class RequestUtils {

    private static final BiConsumer<HttpServletResponse, Response> writeResponse = (httpServletResponse, response) -> {
        try {
            ServletOutputStream outputStream = httpServletResponse.getOutputStream();
            new ObjectMapper().writeValue(outputStream, response);
            outputStream.flush();
        } catch (Exception exception) {
            throw new ApiException(exception.getMessage());
        }
    };

    private static final BiFunction<Exception, HttpStatus, String> errorReason = (exception, httpStatus) -> {
        if (httpStatus.isSameCodeAs(FORBIDDEN)) {
            return "You do not have permission to access this resource";
        }
        if (httpStatus.isSameCodeAs(UNAUTHORIZED)) {
            return "You are not logged in";
        }
        if (exception instanceof DisabledException ||
                exception instanceof LockedException ||
                exception instanceof BadCredentialsException ||
                exception instanceof CredentialsExpiredException ||
                exception instanceof ApiException) {
            return exception.getMessage();
        }
        if (httpStatus.is5xxServerError()) {
            return "An internal server error occurred";
        } else {
            return "An error occurred. Please try again";
        }
    };

    public static Response getResponse(HttpServletRequest request, Map<?, ?> data, String message, HttpStatus status) {
        return new Response(
                LocalDateTime.now().toString(),
                status.value(),
                request.getRequestURI(),
                HttpStatus.valueOf(status.value()),
                message,
                EMPTY,
                data);
    }

    public static void handlerErrorResponse(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        HttpStatus status = determineHttpStatus(exception);
        Response apiResponse = getErrorResponse(request, response, exception, status);
        writeResponse.accept(response, apiResponse);
    }

    private static HttpStatus determineHttpStatus(Exception exception) {
        if (exception instanceof BadCredentialsException) {
            return HttpStatus.UNAUTHORIZED;
        } else if (exception instanceof LockedException) {
            return HttpStatus.FORBIDDEN;
        } else if (exception instanceof DisabledException) {
            return HttpStatus.FORBIDDEN;
        } else if (exception instanceof CredentialsExpiredException) {
            return HttpStatus.UNAUTHORIZED;
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    private static Response getErrorResponse(HttpServletRequest request, HttpServletResponse response, Exception exception, HttpStatus status) {
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setStatus(status.value());
        return new Response(
                LocalDateTime.now().toString(),
                status.value(),
                request.getRequestURI(),
                HttpStatus.valueOf(status.value()),
                errorReason.apply(exception, status),
                getRootCauseMessage(exception),
                Map.of());
    }
}

