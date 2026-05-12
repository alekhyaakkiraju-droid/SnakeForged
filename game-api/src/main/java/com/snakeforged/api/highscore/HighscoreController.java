package com.snakeforged.api.highscore;

import com.snakeforged.domain.DifficultyConfig;
import com.snakeforged.persistence.entity.HighscoreEntry;
import com.snakeforged.persistence.repository.HighscoreRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/highscores")
public class HighscoreController {

    private final HighscoreRepository highscoreRepository;
    private final HighscoreService highscoreService;

    public HighscoreController(HighscoreRepository highscoreRepository,
                               HighscoreService highscoreService) {
        this.highscoreRepository = highscoreRepository;
        this.highscoreService = highscoreService;
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

    @PostMapping
    public ResponseEntity<?> submitHighscore(@Valid @RequestBody HighscoreRequestDTO request,
                                             HttpServletRequest httpRequest) {
        String clientIpHash = hashIp(httpRequest.getRemoteAddr());
        try {
            HighscoreEntry saved = highscoreService.submitScore(request, clientIpHash);
            return ResponseEntity.status(HttpStatus.CREATED).body(HighscoreDTO.from(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Required request parameter 'difficulty' is missing"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private static String hashIp(String ip) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return "unknown";
        }
    }
}
