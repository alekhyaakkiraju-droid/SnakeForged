package com.snakeforged.api.highscore;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HighscoreRequestDTO(

        @NotBlank(message = "{validation.nickname.blank}")
        @Size(max = 20, message = "{validation.nickname.size}")
        @Pattern(regexp = "[A-Za-z0-9_]+", message = "{validation.nickname.pattern}")
        String nickname,

        @NotNull(message = "{validation.score.null}")
        @Min(value = 0, message = "{validation.score.min}")
        Integer score,

        @NotBlank(message = "{validation.difficulty.blank}")
        String difficulty
) {}
