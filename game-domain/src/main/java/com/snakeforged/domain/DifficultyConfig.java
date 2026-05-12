package com.snakeforged.domain;

/**
 * Maps each difficulty level to its tick interval in milliseconds.
 * The caller (timer/scheduler) reads {@link #getTickIntervalMs()} to control
 * how frequently {@code GameEngine.tick()} is invoked — the engine itself is
 * tick-driven and has no internal timer.
 */
public enum DifficultyConfig {
    EASY(100, "Easy"),
    MEDIUM(70, "Medium"),
    HARD(40, "Hard");

    private final int tickIntervalMs;
    private final String displayName;

    DifficultyConfig(int tickIntervalMs, String displayName) {
        this.tickIntervalMs = tickIntervalMs;
        this.displayName = displayName;
    }

    /** Milliseconds between ticks at this difficulty level. */
    public int getTickIntervalMs() { return tickIntervalMs; }

    /** Human-readable name suitable for display in the UI. */
    public String getDisplayName() { return displayName; }

    /**
     * Looks up a {@code DifficultyConfig} by name, case-insensitively.
     *
     * @param name the difficulty name (e.g. "easy", "MEDIUM", "Hard")
     * @return the matching {@code DifficultyConfig}
     * @throws IllegalArgumentException if no match is found
     */
    public static DifficultyConfig fromName(String name) {
        for (DifficultyConfig d : values()) {
            if (d.name().equalsIgnoreCase(name)) {
                return d;
            }
        }
        throw new IllegalArgumentException(
                "Unknown difficulty: '" + name + "'. Valid values: EASY, MEDIUM, HARD");
    }
}
