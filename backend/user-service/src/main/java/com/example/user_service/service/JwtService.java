package com.example.user_service.service;

import com.example.user_service.domain.Token;
import com.example.user_service.domain.TokenData;
import com.example.user_service.dto.User;
import com.example.user_service.enumeration.TokenType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;
import java.util.function.Function;

public interface JwtService {

    String createToken(User user, Function<Token,String> tokenFunction);

    Optional<String> extractToken(HttpServletRequest request, String cookieName);

    void addCookie(HttpServletResponse response, User user, TokenType tokenType);

    <T> T getTokenData(String token, Function<TokenData, T> tokenFunction);

    void removeCookie(HttpServletRequest request, HttpServletResponse response, TokenType tokenType);


}
