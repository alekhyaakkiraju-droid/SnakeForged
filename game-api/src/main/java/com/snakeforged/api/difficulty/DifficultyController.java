package com.snakeforged.api.difficulty;

import com.snakeforged.domain.DifficultyConfig;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/difficulties")
public class DifficultyController {

    private static final List<DifficultyDTO> DIFFICULTIES =
            Arrays.stream(DifficultyConfig.values())
                  .map(DifficultyDTO::from)
                  .toList();

    @GetMapping
    public ResponseEntity<List<DifficultyDTO>> getDifficulties() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS))
                .body(DIFFICULTIES);
    }
}
