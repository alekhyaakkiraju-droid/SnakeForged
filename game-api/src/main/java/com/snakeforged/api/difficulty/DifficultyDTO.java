package com.snakeforged.api.difficulty;

import com.snakeforged.domain.DifficultyConfig;

public record DifficultyDTO(String name, String displayName, int tickIntervalMs) {

    public static DifficultyDTO from(DifficultyConfig config) {
        return new DifficultyDTO(config.name(), config.getDisplayName(), config.getTickIntervalMs());
    }
}
