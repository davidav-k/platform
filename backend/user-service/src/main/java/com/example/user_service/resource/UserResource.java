package com.example.user_service.resource;

import com.example.user_service.domain.Response;
import com.example.user_service.domain.TokenData;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.User;
import com.example.user_service.dto.UserRequest;
import com.example.user_service.enumeration.TokenType;
import com.example.user_service.service.JwtService;
import com.example.user_service.service.UserService;
import com.example.user_service.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;

@Slf4j
@RestController
//@RequestMapping(path = "${api.endpoint.base-url}/user")
@RequestMapping(path = "/api/v1/user")
@RequiredArgsConstructor
public class UserResource {

    private final UserService userService;
    private final JwtService jwtService;

    private URI getUri() {
        return URI.create("");
    }

    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping("/register")
    public ResponseEntity<Response> saveUser(@RequestBody @Valid UserRequest userRequest, HttpServletRequest request) {
        log.info("Creating new user with email: {}", userRequest.getEmail());
        userService.createUser(userRequest.getFirstName(), userRequest.getLastName(), userRequest.getEmail(), userRequest.getPassword());
        log.info("Account created successfully: {}", userRequest.getEmail());
        return ResponseEntity.created(getUri()).body(RequestUtils.getResponse(
                request,
                emptyMap(),
                "Account created. Check your email to enable your account",
                HttpStatus.CREATED));
    }

    @GetMapping("/verify/account")
    public ResponseEntity<Response> verifyNewUserAccount(@RequestParam("key") String key, HttpServletRequest request) {
        log.info("Verifying account with key: {}", key);
        userService.verifyAccountKey(key);
        log.info("Account verified successfully for key: {}", key);
        return ResponseEntity.ok().body(RequestUtils.getResponse(
                request,
                emptyMap(),
                "Account verified successfully.",
                HttpStatus.OK));
    }

    @PostMapping("/login")
    public ResponseEntity<Response> loginUser(@RequestBody @Valid LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        log.info("Logging in user with email into controller: {}", loginRequest.getEmail());
        Authentication authentication = userService.authenticateUser(loginRequest.getEmail(), loginRequest.getPassword(), request);
        log.info("User logged in successfully with email into controller: {}", loginRequest.getEmail());
        jwtService.addCookie(response, (User) authentication.getPrincipal(), TokenType.ACCESS);
        jwtService.addCookie(response, (User) authentication.getPrincipal(), TokenType.REFRESH);
        return ResponseEntity.ok().body(RequestUtils.getResponse(
                request,
                Map.of("user", (User) authentication.getPrincipal()),
                "Login successful into controller.",
                HttpStatus.OK));
    }

    @PreAuthorize("hasAuthority('user:update') or #userId == authentication.principal.id")
    @PostMapping("/enable-mfa")
    public ResponseEntity<Response> enableMfa(@RequestParam String email, HttpServletRequest request) {
        userService.enableMfa(email);
        return ResponseEntity.ok().body(RequestUtils.getResponse(
                request,
                Map.of(),
                "MFA enabled successfully. Scan QR code in Google Authenticator.",
                HttpStatus.OK));
    }

    @PostMapping("/verify-mfa")
    public ResponseEntity<Response> verifyMfa(@RequestParam String email, @RequestParam int code, HttpServletRequest request, HttpServletResponse response) {
        if (userService.verifyMfa(email, code)) {
            User user = userService.getUserByEmail(email);
            jwtService.addCookie(response, user, TokenType.ACCESS);
            jwtService.addCookie(response, user, TokenType.REFRESH);
            return ResponseEntity.ok().body(RequestUtils.getResponse(
                    request,
                    Map.of("user", user),
                    "MFA verification successful",
                    HttpStatus.OK));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(RequestUtils.getResponse(
                    request,
                    Map.of(),
                    "Invalid MFA code",
                    HttpStatus.FORBIDDEN));
        }
    }

    @PreAuthorize("hasAuthority('user:update')")
    @PostMapping("/unlock")
    public ResponseEntity<Response> unlockUser(@RequestParam String email, HttpServletRequest request) {
        userService.unlockedUser(email);
        return ResponseEntity.ok().body(RequestUtils.getResponse(
                request,
                Map.of(),
                "User unlocked successfully.",
                HttpStatus.OK));
    }

    @PreAuthorize("hasAuthority('user:update')")
    @PostMapping("/lock")
    public ResponseEntity<Response> lockUser(@RequestParam String email, HttpServletRequest request) {
        userService.lockedUser(email);
        return ResponseEntity.ok().body(RequestUtils.getResponse(
                request,
                Map.of(),
                "User locked successfully.",
                HttpStatus.OK));
    }



    @PostMapping("/refresh")
    public ResponseEntity<Response> refreshTokens(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> refreshToken = jwtService.extractToken(request, TokenType.REFRESH.getValue());
        if (refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(RequestUtils.getResponse(request, Map.of(), "Unauthorized access", HttpStatus.UNAUTHORIZED));
        }

        User tokenData = jwtService.getTokenData(refreshToken.get(), TokenData::getUser);
        User user = userService.getUserByUserId(tokenData.getUserId());

        jwtService.addCookie(response, user, TokenType.ACCESS);
        jwtService.addCookie(response, user, TokenType.REFRESH);

        return ResponseEntity.ok().body(RequestUtils.getResponse(request, Map.of(), "Tokens refreshed successfully.", HttpStatus.OK));
    }

    @GetMapping("/profile")
    public ResponseEntity<Response> getUserProfile(HttpServletRequest request) {
        Optional<String> optionalAccessToken = jwtService.extractToken(request, TokenType.ACCESS.getValue());
        if (optionalAccessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(RequestUtils.getResponse(request, emptyMap(), "Unauthorized access.", HttpStatus.UNAUTHORIZED));
        }
        var tokenData = jwtService.getTokenData(optionalAccessToken.get(), TokenData::getUser);
        User user = userService.getUserByUserId(tokenData.getUserId());
        return ResponseEntity.ok().body(RequestUtils.getResponse(request, Map.of("user", user), "User profile retrieved successfully.", HttpStatus.OK));
    }

    @PreAuthorize("hasAuthority('user:update') or #userId == authentication.principal.id")
    @PutMapping("/{userId}")
    public ResponseEntity<Response> updateUser(@PathVariable Long userId, @RequestBody @Valid UserRequest userRequest, HttpServletRequest request) {

        log.info("Updating user with ID: {}", userId);
        userService.updateUser(userId,userRequest);

        return ResponseEntity.ok().body(RequestUtils.getResponse(request, emptyMap(), "User updated successfully.", HttpStatus.OK));
    }

    @PreAuthorize("hasAuthority('user:update') or #userId == authentication.principal.id")
    @PatchMapping("/password/{userId}")
    public ResponseEntity<Response> changePassword(@PathVariable Long userId, @RequestBody Map<String, String> passwordMap, HttpServletRequest request) {
        String oldPassword = passwordMap.get("oldPassword");
        String newPassword = passwordMap.get("newPassword");
        String confirmNewPassword = passwordMap.get("confirmNewPassword");

        userService.changePassword(userId, oldPassword, newPassword, confirmNewPassword);
        return ResponseEntity.ok().body(RequestUtils.getResponse(request, emptyMap(), "Changed password successfully.", HttpStatus.OK));
    }

    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Response> deleteUser(@PathVariable Long userId, HttpServletRequest request, Authentication authentication) {

        userService.deleteUser(userId, authentication);
        return ResponseEntity.ok().body(RequestUtils.getResponse(request, emptyMap(), "User deleted successfully.", HttpStatus.OK));
    }
}