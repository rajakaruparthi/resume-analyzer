package com.sprint.analyzer.controller;

import com.sprint.analyzer.entity.User;
import com.sprint.analyzer.entity.req.AuthRequest;
import com.sprint.analyzer.entity.req.RegisterRequest;
import com.sprint.analyzer.entity.req.RefreshTokenRequest; // Import new DTO
import com.sprint.analyzer.entity.response.AuthResponse;
import com.sprint.analyzer.service.JwtBlacklistService;
import com.sprint.analyzer.service.JwtService;
import com.sprint.analyzer.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Add Slf4j for logging
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j // Add Slf4j
@Tag(name = "Authentication", description = "User registration, login, and token management")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final JwtBlacklistService jwtBlacklistService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user and get JWT tokens")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(request);
            UserDetails userDetails = user;

            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    AuthResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .userId(user.getId())
                            .email(user.getEmail())
                            .role(user.getRole().name())
                            .emailVerified(user.isEmailVerified())
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.getUserByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found after authentication"));

            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            return ResponseEntity.ok(
                    AuthResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .userId(user.getId())
                            .email(user.getEmail())
                            .role(user.getRole().name())
                            .emailVerified(user.isEmailVerified())
                            .build()
            );
        } catch (Exception e) { // Catch AuthenticationException and other runtime exceptions
            log.warn("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access and refresh tokens using a valid refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String userEmail;

        try {
            // 1. Check if the refresh token is blacklisted
            if (jwtBlacklistService.isTokenBlacklisted(refreshToken)) {
                log.warn("Attempted to use a blacklisted refresh token.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 2. Extract username from the refresh token
            userEmail = jwtService.extractUsername(refreshToken);
            if (userEmail == null) {
                log.warn("Refresh token has no subject (username).");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 3. Load user details
            UserDetails userDetails = userService.getUserByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found for refresh token."));

            // 4. Validate the refresh token (signature and expiration)
            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                log.warn("Invalid or expired refresh token for user: {}", userEmail);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 5. Blacklist the old refresh token (for rotation)
            jwtBlacklistService.blacklistToken(refreshToken);
            log.info("Old refresh token blacklisted for user: {}", userEmail);

            // 6. Generate new access and refresh tokens
            String newAccessToken = jwtService.generateAccessToken(userDetails);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails);

            User user = userService.getUserByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found after refresh token validation"));

            return ResponseEntity.ok(
                    AuthResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshToken)
                            .userId(user.getId())
                            .email(user.getEmail())
                            .role(user.getRole().name())
                            .emailVerified(user.isEmailVerified())
                            .build()
            );

        } catch (Exception e) {
            log.error("Refresh token failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate the current JWT token (logout)")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            jwtBlacklistService.blacklistToken(jwt);
            SecurityContextHolder.clearContext();
            log.info("User logged out successfully by blacklisting token.");
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        }
        log.warn("Logout attempt without a valid Authorization header.");
        return ResponseEntity.badRequest().body(Map.of("error", "No valid token found in Authorization header"));
    }
}