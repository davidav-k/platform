package com.example.user_service.security;

import com.example.user_service.domain.ApiAuthentication;
import com.example.user_service.domain.UserPrincipal;
import com.example.user_service.dto.User;
import com.example.user_service.entity.CredentialEntity;
import com.example.user_service.exception.ApiException;
import com.example.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Function;
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    private final Function<Authentication, ApiAuthentication> apiAuthenticationFunction = authentication -> (ApiAuthentication) authentication;

    private final Consumer<UserPrincipal> validAccount = userPrincipal -> {
        if (!userPrincipal.isAccountNonLocked()) {
            throw new LockedException("Account is locked");
        }
        if (!userPrincipal.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }
        if (!userPrincipal.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("Your password has expired. Please update your password");
        }
        if (!userPrincipal.isAccountNonExpired()) {
            throw new DisabledException("Account has expired. Please contact the administrator");
        }

    };

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiAuthentication.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiAuthentication apiAuthentication = apiAuthenticationFunction.apply(authentication);
        User user = userService.getUserByEmail(apiAuthentication.getEmail());
        if (user == null) {
            throw new ApiException("Unable to authenticate user");
        }
        CredentialEntity userCredential = userService.getUserCredentialById(user.getId());
//        TODO: enable password expiry
//        if (userCredential.getUpdatedAt().minusDays(NINETY_DAYS).isAfter(LocalDateTime.now())){
//            throw new ApiException("Password expired. Please reset your password");
//        }
        if (!user.isCredentialsNonExpired()){
            throw new ApiException("Password expired. Please reset your password");
        }
        UserPrincipal userPrincipal = new UserPrincipal(user, userCredential);
        validAccount.accept(userPrincipal);
        if (passwordEncoder.matches(apiAuthentication.getPassword(), userCredential.getPassword())){
            log.info("authenticated success into ApiAuthenticationProvider");
            return ApiAuthentication.authenticated(user, userPrincipal.getAuthorities());
        }
        throw new BadCredentialsException("Email and/or password is incorrect. Please try again");
    }

}
