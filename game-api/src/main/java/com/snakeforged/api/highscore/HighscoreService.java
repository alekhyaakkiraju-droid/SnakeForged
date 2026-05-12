package com.snakeforged.api.highscore;

import com.snakeforged.audit.AuditService;
import com.snakeforged.domain.DifficultyConfig;
import com.snakeforged.observability.MetricsService;
import com.snakeforged.persistence.entity.HighscoreEntry;
import com.snakeforged.persistence.repository.HighscoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HighscoreService {

    /**
     * Stub plausibility threshold — reject scores higher than this regardless of difficulty.
     * Should be calibrated to the actual grid size once the frontend grid dimensions are fixed.
     * A 30×30 grid yields a theoretical max of 899; 10 000 provides ample margin while still
     * blocking clearly fabricated values.
     */
    static final int MAX_PLAUSIBLE_SCORE = 10_000;

    private final HighscoreRepository highscoreRepository;
    private final AuditService auditService;
    private final MetricsService metricsService;

    public HighscoreService(HighscoreRepository highscoreRepository,
                            AuditService auditService,
                            MetricsService metricsService) {
        this.highscoreRepository = highscoreRepository;
        this.auditService = auditService;
        this.metricsService = metricsService;
    }

    /**
     * Validates, persists a highscore, and records an audit event.
     *
     * @param request      validated request DTO (bean validation already applied by Spring MVC)
     * @param clientIpHash SHA-256 hex hash of the submitter's IP address
     * @return the persisted {@link HighscoreEntry}
     * @throws IllegalArgumentException if difficulty is unknown or score is implausible
     */
    public HighscoreEntry submitScore(HighscoreRequestDTO request, String clientIpHash) {
        DifficultyConfig config;
        try {
            config = DifficultyConfig.fromName(request.difficulty());
        } catch (IllegalArgumentException e) {
            metricsService.recordRejection();
            throw new IllegalArgumentException(e.getMessage());
        }

        if (request.score() > MAX_PLAUSIBLE_SCORE) {
            metricsService.recordRejection();
            throw new IllegalArgumentException(
                    "Score " + request.score() + " is implausible for difficulty " + config.name());
        }

        HighscoreEntry entry = new HighscoreEntry();
        entry.setNickname(request.nickname());
        entry.setScore(request.score());
        entry.setDifficulty(config.name());
        HighscoreEntry saved = highscoreRepository.save(entry);

        auditService.recordScoreSubmitted(request.nickname(), request.score(), config.name(), clientIpHash);

        metricsService.recordSubmission();
        return saved;
    }
}
