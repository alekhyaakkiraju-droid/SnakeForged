package com.snakeforged.persistence.repository;

import com.snakeforged.persistence.entity.HighscoreEntry;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.enabled=false")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL")
class HighscoreRepositoryIT {

    @Autowired
    private HighscoreRepository repo;

    // ── round-trip ────────────────────────────────────────────────────────────

    @Test
    void saveAndRetrieveRoundTrip() {
        HighscoreEntry e = entry("Alice", 500, "EASY");
        repo.saveAndFlush(e);

        List<HighscoreEntry> top10 = repo.findTop10ByDifficultyOrderByScoreDesc("EASY");
        assertThat(top10).hasSize(1);
        assertThat(top10.get(0).getNickname()).isEqualTo("Alice");
        assertThat(top10.get(0).getScore()).isEqualTo(500);
    }

    @Test
    void topTenReturnsAtMostTenSortedByScoreDesc() {
        for (int i = 1; i <= 12; i++) {
            repo.save(entry("Player" + i, i * 10, "MEDIUM"));
        }
        repo.flush();

        List<HighscoreEntry> top10 = repo.findTop10ByDifficultyOrderByScoreDesc("MEDIUM");
        assertThat(top10).hasSize(10);
        assertThat(top10.get(0).getScore()).isEqualTo(120); // highest first
        assertThat(top10.get(9).getScore()).isEqualTo(30);  // 10th highest
    }

    @Test
    void topTenFiltersOtherDifficulties() {
        repo.save(entry("EasyPlayer", 999, "EASY"));
        repo.save(entry("HardPlayer", 100, "HARD"));
        repo.flush();

        assertThat(repo.findTop10ByDifficultyOrderByScoreDesc("EASY")).hasSize(1);
        assertThat(repo.findTop10ByDifficultyOrderByScoreDesc("HARD")).hasSize(1);
        assertThat(repo.findTop10ByDifficultyOrderByScoreDesc("MEDIUM")).isEmpty();
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test
    void nullNicknameThrowsConstraintViolation() {
        HighscoreEntry e = new HighscoreEntry();
        e.setScore(100);
        e.setDifficulty("EASY");
        // nickname is null → @NotBlank violation
        assertThatThrownBy(() -> repo.saveAndFlush(e))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void nicknameTooLongThrowsConstraintViolation() {
        HighscoreEntry e = entry("A".repeat(21), 100, "EASY");
        assertThatThrownBy(() -> repo.saveAndFlush(e))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void blankNicknameThrowsConstraintViolation() {
        HighscoreEntry e = entry("   ", 100, "EASY");
        assertThatThrownBy(() -> repo.saveAndFlush(e))
                .isInstanceOf(ConstraintViolationException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static HighscoreEntry entry(String nickname, int score, String difficulty) {
        HighscoreEntry e = new HighscoreEntry();
        e.setNickname(nickname);
        e.setScore(score);
        e.setDifficulty(difficulty);
        return e;
    }
}
