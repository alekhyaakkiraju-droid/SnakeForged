package com.snakeforged.api.highscore;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HighscoreRequestDTO(

        @NotBlank(message = "Nickname is required")
        @Size(max = 20, message = "Nickname must be at most 20 characters")
        @Pattern(regexp = "[A-Za-z0-9_]+", message = "Nickname may only contain letters, digits, and underscores")
        String nickname,

        @NotNull(message = "Score is required")
        @Min(value = 0, message = "Score must be non-negative")
        Integer score,

        @NotBlank(message = "Difficulty is required")
        String difficulty
) {}
