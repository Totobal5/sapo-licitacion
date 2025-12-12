package cl.sapo.licitaciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Security configuration for CSRF protection, CORS, and security headers.
 * Implements DevSecOps best practices for Spring Boot 3.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Main security filter chain configuration.
     * - CSRF protection enabled for all POST requests
     * - Security headers (XSS, Frame Options, CSP)
     * - Public access to RSS and health endpoints
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF Protection with cookie repository
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/actuator/**") // Actuator endpoints excluded
            )
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/rss", "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/sync").permitAll() // Public but protected by CSRF
                .anyRequest().permitAll()
            )
            
            // Security Headers
            .headers(headers -> headers
                // Content Security Policy - Prevent XSS
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                                    "style-src 'self' 'unsafe-inline'; " +
                                    "script-src 'self' 'unsafe-inline'; " +
                                    "img-src 'self' data:; " +
                                    "font-src 'self' data:;")
                )
                // X-XSS-Protection header
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                )
                // X-Frame-Options - Prevent clickjacking
                .frameOptions(frame -> frame.deny())
                // X-Content-Type-Options
                .contentTypeOptions(contentType -> {})
            );

        return http.build();
    }

    /**
     * CORS configuration for RSS endpoint.
     * Restricts cross-origin access to specific methods.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/rss")
                        .allowedOrigins("*") // RSS feeds are public
                        .allowedMethods("GET", "HEAD")
                        .maxAge(3600);
                
                // Restrict other endpoints
                registry.addMapping("/sync")
                        .allowedOrigins("http://localhost:8080", "https://yourdomain.com")
                        .allowedMethods("POST")
                        .maxAge(3600);
            }
        };
    }
}
