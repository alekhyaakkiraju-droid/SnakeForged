package com.snakeforged.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snakeforged.api.error.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window in-memory rate limiter for POST /api/v1/highscores.
 * Keyed per client IP. GET endpoints are unaffected.
 */
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMITED_PATH = "/api/v1/highscores";

    private final int requestsPerMinute;
    private final int windowSeconds;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${ratelimit.highscore.requests-per-minute:10}") int requestsPerMinute,
            @Value("${ratelimit.highscore.window-seconds:60}") int windowSeconds,
            MessageSource messageSource,
            ObjectMapper objectMapper) {
        this.requestsPerMinute = requestsPerMinute;
        this.windowSeconds = windowSeconds;
        this.messageSource = messageSource;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !RATE_LIMITED_PATH.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        Locale locale = request.getLocale();
        String ip = request.getRemoteAddr();
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);

        Deque<Instant> timestamps = requestLog.computeIfAbsent(ip, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= requestsPerMinute) {
                Instant oldest = timestamps.peekFirst();
                long retryAfter = Math.max(1,
                        windowSeconds - Duration.between(oldest, now).getSeconds());

                String message = messageSource.getMessage(
                        "error.rate_limit.exceeded",
                        new Object[]{requestsPerMinute, windowSeconds},
                        locale);

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", String.valueOf(retryAfter));
                response.setContentType("application/json;charset=UTF-8");
                String body = objectMapper.writeValueAsString(
                        ErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS.value(), message, request.getRequestURI()));
                response.getWriter().write(body);
                return;
            }

            timestamps.addLast(now);
        }

        chain.doFilter(request, response);
    }
}
