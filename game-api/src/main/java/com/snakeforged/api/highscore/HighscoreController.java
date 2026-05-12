package com.snakeforged.api.highscore;

import com.snakeforged.domain.DifficultyConfig;
import com.snakeforged.persistence.repository.HighscoreRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/highscores")
public class HighscoreController {

    private final HighscoreRepository highscoreRepository;

    public HighscoreController(HighscoreRepository highscoreRepository) {
        this.highscoreRepository = highscoreRepository;
    }

    @GetMapping
    public ResponseEntity<?> getHighscores(@RequestParam String difficulty) {
        DifficultyConfig config;
        try {
            config = DifficultyConfig.fromName(difficulty);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }

        List<HighscoreDTO> scores = highscoreRepository
                .findTop10ByDifficultyOrderByScoreDesc(config.name())
                .stream()
                .map(HighscoreDTO::from)
                .toList();

        return ResponseEntity.ok(scores);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Required request parameter 'difficulty' is missing"));
    }
}
