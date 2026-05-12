package com.snakeforged.api.highscore;

import com.snakeforged.audit.AuditService;
import com.snakeforged.observability.MetricsService;
import com.snakeforged.persistence.entity.HighscoreEntry;
import com.snakeforged.persistence.repository.HighscoreRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HighscoreServiceTest {

    private HighscoreRepository highscoreRepository;
    private AuditService auditService;
    private SimpleMeterRegistry meterRegistry;
    private HighscoreService service;

    @BeforeEach
    void setUp() {
        highscoreRepository = mock(HighscoreRepository.class);
        auditService = mock(AuditService.class);
        meterRegistry = new SimpleMeterRegistry();
        service = new HighscoreService(highscoreRepository, auditService,
                new MetricsService(meterRegistry));
    }

    private double counter(String name) {
        Counter c = meterRegistry.find(name).counter();
        return c == null ? 0.0 : c.count();
    }

    private HighscoreEntry savedEntry(String nickname, int score, String difficulty) {
        HighscoreEntry e = new HighscoreEntry();
        e.setNickname(nickname);
        e.setScore(score);
        e.setDifficulty(difficulty);
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    @Test
    void validSubmissionPersistsHighscore() {
        HighscoreRequestDTO req = new HighscoreRequestDTO("Alice", 100, "EASY");
        when(highscoreRepository.save(any())).thenReturn(savedEntry("Alice", 100, "EASY"));

        HighscoreEntry result = service.submitScore(req, "abc123hash");

        assertThat(result.getNickname()).isEqualTo("Alice");
        assertThat(result.getScore()).isEqualTo(100);
        assertThat(result.getDifficulty()).isEqualTo("EASY");
    }

    @Test
    void validSubmissionCreatesAuditEvent() {
        HighscoreRequestDTO req = new HighscoreRequestDTO("Bob", 200, "MEDIUM");
        when(highscoreRepository.save(any())).thenReturn(savedEntry("Bob", 200, "MEDIUM"));

        service.submitScore(req, "hashedip");

        verify(auditService).recordScoreSubmitted("Bob", 200, "MEDIUM", "hashedip");
    }

    @Test
    void caseInsensitiveDifficultyIsNormalized() {
        HighscoreRequestDTO req = new HighscoreRequestDTO("Carol", 50, "hard");
        when(highscoreRepository.save(any())).thenReturn(savedEntry("Carol", 50, "HARD"));

        HighscoreEntry result = service.submitScore(req, "hash");

        ArgumentCaptor<HighscoreEntry> captor = ArgumentCaptor.forClass(HighscoreEntry.class);
        verify(highscoreRepository).save(captor.capture());
        assertThat(captor.getValue().getDifficulty()).isEqualTo("HARD");
    }

    @Test
    void invalidDifficultyThrowsIllegalArgumentException() {
        HighscoreRequestDTO req = new HighscoreRequestDTO("Dave", 100, "ULTRA");

        assertThatThrownBy(() -> service.submitScore(req, "hash"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ULTRA");
    }

    @Test
    void implausibleScoreThrowsIllegalArgumentException() {
        HighscoreRequestDTO req = new HighscoreRequestDTO("Eve", HighscoreService.MAX_PLAUSIBLE_SCORE + 1, "EASY");

        assertThatThrownBy(() -> service.submitScore(req, "hash"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("implausible");
    }

    @Test
    void maxPlausibleScoreIsAccepted() {
        HighscoreRequestDTO req = new HighscoreRequestDTO("Frank", HighscoreService.MAX_PLAUSIBLE_SCORE, "EASY");
        when(highscoreRepository.save(any())).thenReturn(savedEntry("Frank", HighscoreService.MAX_PLAUSIBLE_SCORE, "EASY"));

        HighscoreEntry result = service.submitScore(req, "hash");

        assertThat(result.getScore()).isEqualTo(HighscoreService.MAX_PLAUSIBLE_SCORE);
    }

    // ── Micrometer counter tests ──────────────────────────────────────────────

    @Test
    void successfulSubmissionIncrementsSubmissionsTotalCounter() {
        when(highscoreRepository.save(any())).thenReturn(savedEntry("Alice", 100, "EASY"));

        service.submitScore(new HighscoreRequestDTO("Alice", 100, "EASY"), "hash");

        assertThat(counter("snakeweb_highscore_submissions_total")).isEqualTo(1.0);
        assertThat(counter("snakeweb_highscore_submissions_rejected_total")).isEqualTo(0.0);
    }

    @Test
    void invalidDifficultyIncrementsRejectedCounter() {
        assertThatThrownBy(() ->
                service.submitScore(new HighscoreRequestDTO("Alice", 100, "INVALID"), "hash"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(counter("snakeweb_highscore_submissions_rejected_total")).isEqualTo(1.0);
        assertThat(counter("snakeweb_highscore_submissions_total")).isEqualTo(0.0);
    }

    @Test
    void implausibleScoreIncrementsRejectedCounter() {
        assertThatThrownBy(() ->
                service.submitScore(
                        new HighscoreRequestDTO("Alice", HighscoreService.MAX_PLAUSIBLE_SCORE + 1, "EASY"), "hash"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(counter("snakeweb_highscore_submissions_rejected_total")).isEqualTo(1.0);
    }

    @Test
    void multipleSuccessfulSubmissionsAccumulateCounter() {
        when(highscoreRepository.save(any())).thenReturn(savedEntry("X", 1, "EASY"));

        service.submitScore(new HighscoreRequestDTO("X", 1, "EASY"), "h1");
        service.submitScore(new HighscoreRequestDTO("X", 2, "MEDIUM"), "h2");
        service.submitScore(new HighscoreRequestDTO("X", 3, "HARD"), "h3");

        assertThat(counter("snakeweb_highscore_submissions_total")).isEqualTo(3.0);
    }
}
