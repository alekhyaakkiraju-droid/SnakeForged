package com.snakeforged.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    // Use limit=3 for fast tests
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(3, 60);
    }

    private MockHttpServletRequest postHighscores(String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/highscores");
        req.setRemoteAddr(ip);
        return req;
    }

    private MockHttpServletRequest getHighscores(String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/highscores");
        req.setRemoteAddr(ip);
        return req;
    }

    @Test
    void requestsUnderLimitPassThrough() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            filter.doFilterInternal(postHighscores("10.0.0.1"), response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(chain.getRequest()).isNotNull();
        }
    }

    @Test
    void eleventhRequestReturns429() throws Exception {
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(postHighscores("10.0.0.2"), new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(postHighscores("10.0.0.2"), response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void blockedResponseIncludesRetryAfterHeader() throws Exception {
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(postHighscores("10.0.0.3"), new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(postHighscores("10.0.0.3"), response, new MockFilterChain());
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(Integer.parseInt(response.getHeader("Retry-After"))).isGreaterThan(0);
    }

    @Test
    void blockedResponseBodyContainsRequiredFields() throws Exception {
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(postHighscores("10.0.0.4"), new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(postHighscores("10.0.0.4"), response, new MockFilterChain());
        String body = response.getContentAsString();
        assertThat(body).contains("\"timestamp\"");
        assertThat(body).contains("\"status\"");
        assertThat(body).contains("\"message\"");
        assertThat(body).contains("\"path\"");
    }

    @Test
    void getRequestsAreNotRateLimited() throws Exception {
        // Exhaust POST limit
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(postHighscores("10.0.0.5"), new MockHttpServletResponse(), new MockFilterChain());
        }
        // GET on the same IP should still pass through
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilterInternal(getHighscores("10.0.0.5"), response, chain);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void rateLimitStateIsPerIp() throws Exception {
        // Exhaust limit for IP A
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(postHighscores("10.0.0.6"), new MockHttpServletResponse(), new MockFilterChain());
        }
        // IP B should still be allowed
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilterInternal(postHighscores("10.0.0.7"), response, chain);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }
}
