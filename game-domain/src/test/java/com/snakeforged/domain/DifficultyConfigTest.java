package com.snakeforged.domain;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DifficultyConfigTest {

    // ── tick intervals ────────────────────────────────────────────────────────

    @Test
    void easyTickIntervalIs100Ms() {
        assertThat(DifficultyConfig.EASY.getTickIntervalMs()).isEqualTo(100);
    }

    @Test
    void mediumTickIntervalIs70Ms() {
        assertThat(DifficultyConfig.MEDIUM.getTickIntervalMs()).isEqualTo(70);
    }

    @Test
    void hardTickIntervalIs40Ms() {
        assertThat(DifficultyConfig.HARD.getTickIntervalMs()).isEqualTo(40);
    }

    // ── display names ─────────────────────────────────────────────────────────

    @Test
    void easyDisplayName() {
        assertThat(DifficultyConfig.EASY.getDisplayName()).isEqualTo("Easy");
    }

    @Test
    void mediumDisplayName() {
        assertThat(DifficultyConfig.MEDIUM.getDisplayName()).isEqualTo("Medium");
    }

    @Test
    void hardDisplayName() {
        assertThat(DifficultyConfig.HARD.getDisplayName()).isEqualTo("Hard");
    }

    // ── fromName ──────────────────────────────────────────────────────────────

    @Test
    void fromNameUpperCase() {
        assertThat(DifficultyConfig.fromName("EASY")).isEqualTo(DifficultyConfig.EASY);
    }

    @Test
    void fromNameLowerCase() {
        assertThat(DifficultyConfig.fromName("medium")).isEqualTo(DifficultyConfig.MEDIUM);
    }

    @Test
    void fromNameMixedCase() {
        assertThat(DifficultyConfig.fromName("Hard")).isEqualTo(DifficultyConfig.HARD);
    }

    @Test
    void fromNameInvalidThrows() {
        assertThatThrownBy(() -> DifficultyConfig.fromName("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid")
                .hasMessageContaining("EASY, MEDIUM, HARD");
    }

    @Test
    void fromNameEmptyStringThrows() {
        assertThatThrownBy(() -> DifficultyConfig.fromName(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── GameEngine integration ────────────────────────────────────────────────

    @Test
    void gameEngineAcceptsDifficultyInConstructor() {
        GameEngine e = new GameEngine(10, 10, DifficultyConfig.HARD, new Random(0));
        assertThat(e.getDifficulty()).isEqualTo(DifficultyConfig.HARD);
    }

    @Test
    void gameEngineDefaultsToMediumWhenNoExplicitDifficulty() {
        GameEngine e = new GameEngine(10, 10, new Random(0));
        assertThat(e.getDifficulty()).isEqualTo(DifficultyConfig.MEDIUM);
    }

    @Test
    void gameEngineDifficultyConstructorWithoutRandom() {
        GameEngine e = new GameEngine(10, 10, DifficultyConfig.EASY);
        assertThat(e.getDifficulty()).isEqualTo(DifficultyConfig.EASY);
    }

    // ── exactly three values ──────────────────────────────────────────────────

    @Test
    void exactlyThreeValues() {
        assertThat(DifficultyConfig.values()).hasSize(3);
    }
}
