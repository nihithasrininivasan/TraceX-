package com.traceroute.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceroute.exception.ApiError;
import com.traceroute.repository.UserRepository;
import com.traceroute.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return email -> {
            var user = users.findByEmail(UserService.normalizeEmail(email))
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                    .password(user.getPasswordHash()).roles(user.getRole().name()).build();
        };
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService details, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(details);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager manager, ObjectMapper mapper) throws Exception {
        return http
                // Local MVP: credentials are explicitly supplied, with no session/auth cookie.
                // Re-enable CSRF protection if browser-managed credentials or cookies are introduced.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authenticationManager(manager)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/").hasRole("ADMIN")
                        .requestMatchers("/api/users/**", "/api/incidents/**", "/api/graph/**", "/api/network/**", "/api/relationships/**").authenticated()
                        .anyRequest().denyAll())
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, exception) -> {
                    response.setHeader("WWW-Authenticate", "Basic realm=\"TraceRoute\"");
                    error(mapper, response, 401, "Authentication required");
                }))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader("WWW-Authenticate", "Basic realm=\"TraceRoute\"");
                            error(mapper, response, 401, "Authentication required");
                        })
                        .accessDeniedHandler((request, response, exception) -> error(mapper, response, 403, "Access denied")))
                .build();
    }
    private static void error(ObjectMapper mapper, HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(), new ApiError(status, message));
    }
    @Bean CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
