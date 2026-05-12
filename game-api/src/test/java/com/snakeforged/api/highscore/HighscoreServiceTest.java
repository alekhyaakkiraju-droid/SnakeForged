package com.snakeforged.api.highscore;

import com.snakeforged.persistence.entity.AuditEvent;
import com.snakeforged.persistence.entity.HighscoreEntry;
import com.snakeforged.persistence.repository.AuditEventRepository;
import com.snakeforged.persistence.repository.HighscoreRepository;
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
    private AuditEventRepository auditEventRepository;
    private HighscoreService service;

    @BeforeEach
    void setUp() {
        highscoreRepository = mock(HighscoreRepository.class);
        auditEventRepository = mock(AuditEventRepository.class);
        service = new HighscoreService(highscoreRepository, auditEventRepository);
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
        when(auditEventRepository.save(any())).thenReturn(new AuditEvent());

        HighscoreEntry result = service.submitScore(req, "abc123hash");

        assertThat(result.getNickname()).isEqualTo("Alice");
        assertThat(result.getScore()).isEqualTo(100);
        assertThat(result.getDifficulty()).isEqualTo("EASY");
    }

    @Test
    void validSubmissionCreatesAuditEvent() {
        HighscoreRequestDTO req = new HighscoreRequestDTO("Bob", 200, "MEDIUM");
        when(highscoreRepository.save(any())).thenReturn(savedEntry("Bob", 200, "MEDIUM"));
        when(auditEventRepository.save(any())).thenReturn(new AuditEvent());

        service.submitScore(req, "hashedip");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent audit = captor.getValue();
        assertThat(audit.getEventType()).isEqualTo("SCORE_SUBMITTED");
        assertThat(audit.getNickname()).isEqualTo("Bob");
        assertThat(audit.getScore()).isEqualTo(200);
        assertThat(audit.getDifficulty()).isEqualTo("MEDIUM");
        assertThat(audit.getClientIpHash()).isEqualTo("hashedip");
    }

    @Test
    void caseInsensitiveDifficultyIsNormalized() {
        HighscoreRequestDTO req = new HighscoreRequestDTO("Carol", 50, "hard");
        when(highscoreRepository.save(any())).thenReturn(savedEntry("Carol", 50, "HARD"));
        when(auditEventRepository.save(any())).thenReturn(new AuditEvent());

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
        when(auditEventRepository.save(any())).thenReturn(new AuditEvent());

        HighscoreEntry result = service.submitScore(req, "hash");

        assertThat(result.getScore()).isEqualTo(HighscoreService.MAX_PLAUSIBLE_SCORE);
    }
}
