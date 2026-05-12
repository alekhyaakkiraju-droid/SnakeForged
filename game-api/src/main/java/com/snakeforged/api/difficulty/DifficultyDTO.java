package com.snakeforged.api.difficulty;

import com.snakeforged.domain.DifficultyConfig;
import org.springframework.context.MessageSource;

import java.util.Locale;

public record DifficultyDTO(String name, String displayName, int tickIntervalMs) {

    public static DifficultyDTO from(DifficultyConfig config, MessageSource messages, Locale locale) {
        String display = messages.getMessage("difficulty.display." + config.name(), null, locale);
        return new DifficultyDTO(config.name(), display, config.getTickIntervalMs());
    }
}
