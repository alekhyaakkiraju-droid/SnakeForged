package com.snakeforged;

import com.snakeforged.api.highscore.HighscoreRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for rate limiting behaviour.
 * Uses a separate Spring context with a limit of 2 req/min so only 3 requests
 * are needed to trigger a 429 — much faster than the production value of 10.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "ratelimit.highscore.requests-per-minute=2",
                "ratelimit.highscore.window-seconds=60"
        }
)
class RateLimitIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void postHighscoreExceedingRateLimitReturns429WithStructuredError() {
        HighscoreRequestDTO req = new HighscoreRequestDTO("Limiter", 10, "EASY");

        // First two requests should succeed (limit = 2)
        ResponseEntity<String> r1 = restTemplate.postForEntity(
                url("/api/v1/highscores"), req, String.class);
        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> r2 = restTemplate.postForEntity(
                url("/api/v1/highscores"), req, String.class);
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Third request must be rejected
        ResponseEntity<String> r3 = restTemplate.postForEntity(
                url("/api/v1/highscores"), req, String.class);
        assertThat(r3.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(r3.getBody()).contains("429");
    }
}
