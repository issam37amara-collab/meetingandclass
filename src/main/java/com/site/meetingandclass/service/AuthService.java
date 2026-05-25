package com.site.meetingandclass.service;

import com.site.meetingandclass.dto.AuthResponse;
import com.site.meetingandclass.dto.LoginRequest;
import com.site.meetingandclass.dto.SignupRequest;
import com.site.meetingandclass.enums.AccountStatus;
import com.site.meetingandclass.enums.Role;
import com.site.meetingandclass.model.User;
import com.site.meetingandclass.repository.UserRepository;
import com.site.meetingandclass.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles user registration and login.
 * Enforces the @cuniv-naama.dz email domain restriction.
 */
@Service
public class AuthService {

    private static final String ALLOWED_EMAIL_DOMAIN = "@cuniv-naama.dz";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    // ── SIGNUP ──────────────────────────────────────────────────────────────

    public String signup(SignupRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();

        // 1. Enforce institutional email domain (case-insensitive)
        if (!email.endsWith(ALLOWED_EMAIL_DOMAIN)) {
            throw new IllegalArgumentException(
                "Only institutional emails ending with " + ALLOWED_EMAIL_DOMAIN + " are allowed."
            );
        }

        // 2. Check for duplicate email
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        // 3. SUPER_ADMIN cannot be self-registered — only seeded manually
        if (request.getRole() == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("SUPER_ADMIN accounts cannot be created via signup.");
        }

        // 4. Build and save the new user (account starts PENDING, disabled)
        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus(AccountStatus.PENDING);
        user.setEnabled(false);  // enabled only after approval

        userRepository.save(user);

        return "Registration successful. Your account is pending approval by an administrator.";
    }

    // ── LOGIN ───────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();

        // Spring Security validates credentials and throws BadCredentialsException
        // (also for unknown users thanks to setHideUserNotFoundExceptions(true)).
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userRepository.findByEmail(email)
                // Should never happen — auth succeeded — but stay generic if it does.
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                    "Invalid credentials."));

        // Extra guards (DaoAuthenticationProvider already enforces enabled/locked,
        // but we re-check defensively in case the user object changed mid-flight).
        if (user.getStatus() == AccountStatus.PENDING) {
            throw new org.springframework.security.authentication.DisabledException(
                "Account pending approval.");
        }
        if (user.getStatus() == AccountStatus.REJECTED) {
            throw new org.springframework.security.authentication.LockedException(
                "Account rejected.");
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(
            token,
            user.getRole().name(),
            user.getFullName(),
            user.getEmail(),
            user.getStatus().name()
        );
    }
}
