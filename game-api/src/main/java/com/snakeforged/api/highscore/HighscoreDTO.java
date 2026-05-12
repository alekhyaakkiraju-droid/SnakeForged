package com.snakeforged.api.highscore;

import com.snakeforged.persistence.entity.HighscoreEntry;

import java.time.format.DateTimeFormatter;

public record HighscoreDTO(String nickname, int score, String difficulty, String createdAt) {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static HighscoreDTO from(HighscoreEntry entry) {
        return new HighscoreDTO(
                entry.getNickname(),
                entry.getScore(),
                entry.getDifficulty(),
                entry.getCreatedAt().format(ISO)
        );
    }
}
