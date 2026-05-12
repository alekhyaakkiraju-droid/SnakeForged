package com.snakeforged.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

/**
 * Rejects POST requests to the highscores endpoint whose body exceeds
 * {@code app.request.max-body-bytes} (default 4096). Checks the
 * Content-Length header first (fast path) then enforces the limit on
 * the actual read stream (covers chunked transfer encoding).
 *
 * Runs at {@link Order} 1, before {@link RateLimitFilter} (Order 2), so
 * oversized payloads are rejected before consuming a rate-limit slot.
 */
@Component
@Order(1)
public class RequestBodySizeLimitFilter extends OncePerRequestFilter {

    private static final String PROTECTED_PATH = "/api/v1/highscores";

    private final long maxBodyBytes;

    public RequestBodySizeLimitFilter(
            @Value("${app.request.max-body-bytes:4096}") long maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !PROTECTED_PATH.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        // Fast path: Content-Length header present and already over limit
        long contentLength = request.getContentLengthLong();
        if (contentLength > maxBodyBytes) {
            reject(request, response);
            return;
        }

        // Slow path: wrap stream to enforce limit even for chunked encoding
        HttpServletRequest wrapped = new SizeLimitedRequestWrapper(request, maxBodyBytes);
        try {
            chain.doFilter(wrapped, response);
        } catch (PayloadTooLargeException e) {
            reject(request, response);
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":413,\"message\":\"Request body exceeds the %d byte limit\",\"path\":\"%s\"}",
                Instant.now(), maxBodyBytes, request.getRequestURI()));
    }

    // ── Wrapper ───────────────────────────────────────────────────────────────

    private static class SizeLimitedRequestWrapper extends HttpServletRequestWrapper {

        private final long limit;
        private ServletInputStream limitedStream;

        SizeLimitedRequestWrapper(HttpServletRequest request, long limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (limitedStream == null) {
                limitedStream = new LimitedServletInputStream(super.getInputStream(), limit);
            }
            return limitedStream;
        }
    }

    private static class LimitedServletInputStream extends ServletInputStream {

        private final InputStream delegate;
        private final long limit;
        private long bytesRead = 0;

        LimitedServletInputStream(InputStream delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b != -1 && ++bytesRead > limit) {
                throw new PayloadTooLargeException();
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = delegate.read(b, off, len);
            if (n > 0) {
                bytesRead += n;
                if (bytesRead > limit) {
                    throw new PayloadTooLargeException();
                }
            }
            return n;
        }

        @Override public boolean isFinished() { return false; }
        @Override public boolean isReady()    { return true; }
        @Override public void setReadListener(ReadListener listener) {}
    }

    static class PayloadTooLargeException extends IOException {
        PayloadTooLargeException() { super("Request body exceeds size limit"); }
    }
}
