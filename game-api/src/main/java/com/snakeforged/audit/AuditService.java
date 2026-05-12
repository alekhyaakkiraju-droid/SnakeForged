package com.snakeforged.audit;

import com.snakeforged.persistence.entity.AuditEvent;
import com.snakeforged.persistence.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central service for writing immutable audit records.
 * All mutation paths in the application go through this service;
 * no component outside this package holds a direct reference to
 * {@link AuditEventRepository}.
 */
@Service
@Transactional
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Records a successful highscore submission.
     *
     * @param nickname      validated player nickname
     * @param score         submitted score value
     * @param difficulty    normalised difficulty name (EASY / MEDIUM / HARD)
     * @param clientIpHash  SHA-256 hex hash of the submitter's IP — raw IP is never stored
     */
    public void recordScoreSubmitted(String nickname, int score, String difficulty, String clientIpHash) {
        AuditEvent event = new AuditEvent();
        event.setEventType("SCORE_SUBMITTED");
        event.setNickname(nickname);
        event.setScore(score);
        event.setDifficulty(difficulty);
        event.setClientIpHash(clientIpHash);
        auditEventRepository.save(event);
    }

    /** Records that the application has finished starting up and is ready to serve traffic. */
    public void recordApplicationStarted() {
        AuditEvent event = new AuditEvent();
        event.setEventType("APPLICATION_STARTED");
        auditEventRepository.save(event);
    }

    /** Records that the application context is shutting down. */
    public void recordApplicationStopped() {
        AuditEvent event = new AuditEvent();
        event.setEventType("APPLICATION_STOPPED");
        auditEventRepository.save(event);
    }
}
