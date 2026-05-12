package com.snakeforged.api.difficulty;

import com.snakeforged.domain.DifficultyConfig;
import org.springframework.context.MessageSource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/difficulties")
public class DifficultyController {

    private final MessageSource messageSource;

    public DifficultyController(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @GetMapping
    public ResponseEntity<List<DifficultyDTO>> getDifficulties(Locale locale) {
        List<DifficultyDTO> difficulties = Arrays.stream(DifficultyConfig.values())
                .map(c -> DifficultyDTO.from(c, messageSource, locale))
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS))
                .body(difficulties);
    }
}
