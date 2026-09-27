package com.uptiq.api.controller;

import com.uptiq.api.dto.*;
import com.uptiq.api.security.CurrentUserProvider;
import com.uptiq.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("")
public class AuthController {

    private static final String COOKIE_NAME = "accessToken";

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    public AuthController(
            UserService userService,
            CurrentUserProvider currentUserProvider
    ) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        UUID id = userService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new SignupResponse(id));
    }

    @PostMapping("/signin")
    public ResponseEntity<Void> signin(
            @Valid @RequestBody SigninRequest request
    ) {
        String token = userService.signin(request);

        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false) // true in production with HTTPS
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(1))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {

        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false) // true in production
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        UUID userId = currentUserProvider.getCurrentUserId();
        UserResponse response = userService.getCurrentUser(userId);

        return ResponseEntity.ok(response);
    }
}