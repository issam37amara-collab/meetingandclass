package com.site.meetingandclass.controller;

import com.site.meetingandclass.dto.AuthResponse;
import com.site.meetingandclass.dto.LoginRequest;
import com.site.meetingandclass.dto.SignupRequest;
import com.site.meetingandclass.dto.UserResponse;
import com.site.meetingandclass.model.User;
import com.site.meetingandclass.security.JwtService;
import com.site.meetingandclass.security.LoginAttemptService;
import com.site.meetingandclass.security.TokenBlacklist;
import com.site.meetingandclass.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    /**
     * POST /api/auth/signup
     * Body: { "fullName":"...", "email":"...@cuniv-naama.dz", "password":"...", "role":"TEACHER" }
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        String message = authService.signup(request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    /**
     * POST /api/auth/login
     * Body: { "email":"...", "password":"..." }
     * Always returns the same generic error on failure to avoid leaking
     * whether the email exists.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String email = request.getEmail();

        if (loginAttemptService.isLocked(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "status", 429,
                "error", "Too Many Requests",
                "message", "Too many failed attempts. Try again in "
                    + loginAttemptService.getLockDurationMinutes() + " minutes."
            ));
        }

        try {
            AuthResponse response = authService.login(request);
            loginAttemptService.loginSucceeded(email);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException | org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            loginAttemptService.loginFailed(email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", 401, "error", "Unauthorized",
                            "message", "Invalid credentials."));
        } catch (LockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", 403, "error", "Forbidden",
                            "message", "Your account has been rejected. Please contact the administrator."));
        } catch (org.springframework.security.authentication.DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", 403, "error", "Forbidden",
                            "message", "Your account is pending approval."));
        }
    }

    /**
     * POST /api/auth/logout
     * Adds the presented token's jti to the blacklist so subsequent requests
     * with the same token are rejected even though it hasn't expired yet.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String jti = jwtService.extractJti(token);
                Date exp = jwtService.extractExpiration(token);
                if (jti != null && exp != null) {
                    tokenBlacklist.revoke(jti, exp.getTime());
                }
            } catch (Exception ignored) {
                // Malformed token: nothing to revoke; treat as success.
            }
        }
        return ResponseEntity.ok(Map.of("message", "Logged out."));
    }

    /**
     * GET /api/auth/me
     * Returns the profile of the currently authenticated user.
     * Requires: Authorization: Bearer <token>
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new UserResponse(user));
    }
}
