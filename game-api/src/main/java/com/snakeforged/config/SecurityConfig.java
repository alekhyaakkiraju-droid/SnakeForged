package com.snakeforged.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configurable operator token used for HTTP Basic auth on /actuator/prometheus.
     * Set via the OPERATOR_TOKEN environment variable in production; defaults to a
     * dev-only placeholder that is never used in CI or production environments.
     */
    @Value("${app.metrics.operator-token:changeme-dev-only}")
    private String operatorToken;

    /**
     * In-memory user for Prometheus scraper access.
     * Username: {@code prometheus}, password: {@code ${app.metrics.operator-token}}.
     */
    @Bean
    public UserDetailsService operatorUserDetailsService() {
        UserDetails operator = User.withUsername("prometheus")
                .password("{noop}" + operatorToken)
                .roles("ACTUATOR")
                .build();
        return new InMemoryUserDetailsManager(operator);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Prometheus scrape endpoint requires operator credentials
                .requestMatchers("/actuator/prometheus").hasRole("ACTUATOR")
                // All other endpoints — API, health, static assets — are public
                .anyRequest().permitAll())
            .httpBasic(Customizer.withDefaults())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers
                // Disable Spring Security's default Cache-Control so controllers can set their own
                .cacheControl(cacheControl -> cacheControl.disable())
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'"))
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(contentTypeOptions -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true))
                .referrerPolicy(referrer ->
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            );

        return http.build();
    }

    /**
     * Restricts CORS to same-origin only by allowing no external origins.
     * Same-origin browser requests bypass CORS entirely and succeed normally.
     * Cross-origin requests (different scheme/host/port) are rejected at the CORS layer.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Collections.emptyList());
        config.setAllowedOriginPatterns(Collections.emptyList());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
