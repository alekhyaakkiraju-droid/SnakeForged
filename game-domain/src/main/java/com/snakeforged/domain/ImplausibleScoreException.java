package com.snakeforged.domain;

/**
 * Raised when a submitted score violates server-side plausibility rules.
 * The HTTP layer translates this via {@code MessageSource} for user-visible text.
 */
public final class ImplausibleScoreException extends RuntimeException {

    private final int score;
    private final String difficulty;

    public ImplausibleScoreException(int score, String difficulty) {
        this.score = score;
        this.difficulty = difficulty;
    }

    public int getScore() {
        return score;
    }

    public String getDifficulty() {
        return difficulty;
    }
}
