package com.site.meetingandclass.config;

import com.site.meetingandclass.security.JwtAuthenticationEntryPoint;
import com.site.meetingandclass.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired private JwtAuthenticationFilter jwtAuthFilter;
    @Autowired private JwtAuthenticationEntryPoint jwtEntryPoint;
    @Autowired private UserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsCsv;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                       // safe: stateless JWT API
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtEntryPoint))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // ── HTTP Security Headers ───────────────────────────────────────
            .headers(headers -> headers
                .contentTypeOptions(opt -> {})                  // X-Content-Type-Options: nosniff
                .frameOptions(frame -> frame.deny())            // X-Frame-Options: DENY
                .xssProtection(xss -> {})                       // X-XSS-Protection: 0 (modern default)
                .referrerPolicy(rp -> rp.policy(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "img-src 'self' data: https:; " +
                    // Google Fonts CSS + AOS CSS from unpkg.
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://unpkg.com; " +
                    "style-src-elem 'self' 'unsafe-inline' https://fonts.googleapis.com https://unpkg.com; " +
                    // Actual font files come from fonts.gstatic.com.
                    "font-src 'self' data: https://fonts.gstatic.com; " +
                    // AOS, Spline viewer, etc.
                    "script-src 'self' 'unsafe-inline' https://unpkg.com https://prod.spline.design; " +
                    "script-src-elem 'self' 'unsafe-inline' https://unpkg.com https://prod.spline.design; " +
                    // Spline viewer streams its scene over https.
                    "connect-src 'self' https://prod.spline.design https://unpkg.com; " +
                    "frame-ancestors 'none'; " +
                    "base-uri 'self'; " +
                    "form-action 'self'"
                ))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
            )
            .authorizeHttpRequests(auth -> auth
                // ── Auth API endpoints (public) ─────────────────────────────
                .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()

                // ── Public landing/auth pages and global static assets ──────
                // NOTE: Role-restricted dashboards (super-admin-dashboard.html,
                // dos-dashboard.html, etc.) are intentionally NOT in this list.
                // Static templates have no role check possible at the HTTP layer
                // (they're inert HTML), so we rely on the dashboard JS calling
                // /api/auth/me on load and redirecting unauthorised users.
                // The real protection is on every /api/** endpoint below.
                // Static templates are unauthenticated. Both casings of
                // "Rooms.html" / "rooms.html" are accepted because the existing
                // navbars in the templates use both spellings.
                .requestMatchers(
                    "/", "/login", "/signup", "/index.html", "/login.html", "/signup.html",
                    "/pending-approval.html", "/unauthorized.html",
                    "/menu.html", "/super-admin-dashboard.html",
                    "/dos-dashboard.html", "/director-dashboard.html",
                    "/hod-dashboard.html", "/teacher-dashboard.html",
                    "/Rooms.html", "/Rooms2.html",
                    "/rooms.html", "/rooms2.html",
                    "/timetable.html",
                    "/class-timetable.html", "/summary.html",
                    "/admin-reservations.html",
                    "/*.css", "/*.js", "/*.png", "/*.jpg", "/*.jpeg",
                    "/*.ico", "/*.svg", "/*.woff", "/*.woff2",
                    "/favicon.ico", "/error"
                ).permitAll()

                // ── Everything else (every /api/** call) requires JWT ───────
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Don't leak whether the user exists vs wrong password.
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Lock down to explicit origins from configuration.
        List<String> origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
