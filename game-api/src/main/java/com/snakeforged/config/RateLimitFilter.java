package com.snakeforged.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
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

    // Per-IP sliding window of request timestamps
    private final ConcurrentHashMap<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${ratelimit.highscore.requests-per-minute:10}") int requestsPerMinute,
            @Value("${ratelimit.highscore.window-seconds:60}") int windowSeconds) {
        this.requestsPerMinute = requestsPerMinute;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !RATE_LIMITED_PATH.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);

        Deque<Instant> timestamps = requestLog.computeIfAbsent(ip, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Evict entries outside the current window
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= requestsPerMinute) {
                Instant oldest = timestamps.peekFirst();
                long retryAfter = Math.max(1,
                        windowSeconds - Duration.between(oldest, now).getSeconds());

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", String.valueOf(retryAfter));
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(String.format(
                        "{\"timestamp\":\"%s\",\"status\":429,\"message\":\"Rate limit exceeded — max %d requests per %d seconds\",\"path\":\"%s\"}",
                        now, requestsPerMinute, windowSeconds, request.getRequestURI()));
                return;
            }

            timestamps.addLast(now);
        }

        chain.doFilter(request, response);
    }
}
