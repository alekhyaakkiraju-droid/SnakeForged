package com.snakeforged;

import com.snakeforged.api.highscore.HighscoreRequestDTO;
import com.snakeforged.persistence.repository.AuditEventRepository;
import com.snakeforged.persistence.repository.HighscoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end API integration tests exercising the full HTTP→controller→service→repository→DB path.
 * Rate limit is raised to 100 req/min so the test suite itself never hits it.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ratelimit.highscore.requests-per-minute=100"
)
class ApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private HighscoreRepository highscoreRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @BeforeEach
    void cleanHighscores() {
        highscoreRepository.deleteAll();
    }

    // ── Difficulties ─────────────────────────────────────────────────────────

    @Test
    void getDifficultiesReturns200WithThreeDifficulties() {
        ResponseEntity<List> response = restTemplate.getForEntity(
                url("/api/v1/difficulties"), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getDifficultiesResponseContainsExpectedFields() {
        ResponseEntity<List> response = restTemplate.getForEntity(
                url("/api/v1/difficulties"), List.class);

        List<Map<String, Object>> body = response.getBody();
        assertThat(body).isNotNull();
        Map<String, Object> first = body.get(0);
        assertThat(first).containsKeys("name", "displayName", "tickIntervalMs");
    }

    // ── POST /api/v1/highscores ───────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void postHighscoreReturns201AndResponseContainsSubmittedData() {
        HighscoreRequestDTO req = new HighscoreRequestDTO("Alice", 100, "EASY");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/v1/highscores"), req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
                .containsEntry("nickname", "Alice")
                .containsEntry("score", 100)
                .containsEntry("difficulty", "EASY");
    }

    @Test
    @SuppressWarnings("unchecked")
    void postHighscoreDataIsPersistedAndRetrievableViaGet() {
        restTemplate.postForEntity(url("/api/v1/highscores"),
                new HighscoreRequestDTO("Bob", 200, "MEDIUM"), Map.class);

        ResponseEntity<List> get = restTemplate.getForEntity(
                url("/api/v1/highscores?difficulty=MEDIUM"), List.class);

        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get.getBody()).hasSize(1);

        Map<String, Object> entry = (Map<String, Object>) get.getBody().get(0);
        assertThat(entry).containsEntry("nickname", "Bob").containsEntry("score", 200);
    }

    // ── GET /api/v1/highscores ────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getHighscoresReturnsAtMostTenResultsOrderedByScoreDescending() {
        // Insert 15 scores with values 10, 20, …, 150
        for (int i = 1; i <= 15; i++) {
            restTemplate.postForEntity(url("/api/v1/highscores"),
                    new HighscoreRequestDTO("Player" + i, i * 10, "HARD"), Map.class);
        }

        ResponseEntity<List> response = restTemplate.getForEntity(
                url("/api/v1/highscores?difficulty=HARD"), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> body = response.getBody();
        assertThat(body).hasSize(10);

        // Scores must be in non-ascending order
        List<Integer> scores = body.stream()
                .map(m -> (Integer) m.get("score"))
                .toList();
        for (int i = 0; i < scores.size() - 1; i++) {
            assertThat(scores.get(i)).isGreaterThanOrEqualTo(scores.get(i + 1));
        }
        // Highest should be 150 (player 15 × 10)
        assertThat(scores.get(0)).isEqualTo(150);
    }

    // ── Validation errors (400) ───────────────────────────────────────────────

    @Test
    void postHighscoreWithInvalidNicknameReturns400() {
        HighscoreRequestDTO req = new HighscoreRequestDTO("bad nickname!", 100, "EASY");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/v1/highscores"), req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void postHighscoreWithNegativeScoreReturns400() {
        HighscoreRequestDTO req = new HighscoreRequestDTO("Alice", -1, "EASY");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/v1/highscores"), req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void postHighscoreWithUnknownDifficultyReturns400() {
        HighscoreRequestDTO req = new HighscoreRequestDTO("Alice", 100, "ULTRA");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/v1/highscores"), req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Audit logging ─────────────────────────────────────────────────────────

    @Test
    void auditEventIsCreatedAfterSuccessfulScoreSubmission() {
        long countBefore = auditEventRepository.count();

        restTemplate.postForEntity(url("/api/v1/highscores"),
                new HighscoreRequestDTO("Audited", 50, "MEDIUM"), Map.class);

        assertThat(auditEventRepository.count()).isEqualTo(countBefore + 1);
    }
}
