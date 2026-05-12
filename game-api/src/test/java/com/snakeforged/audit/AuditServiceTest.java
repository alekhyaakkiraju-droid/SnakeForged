package com.snakeforged.audit;

import com.snakeforged.persistence.entity.AuditEvent;
import com.snakeforged.persistence.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    private AuditEventRepository auditEventRepository;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditEventRepository = mock(AuditEventRepository.class);
        when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        auditService = new AuditService(auditEventRepository);
    }

    private AuditEvent capturedEvent() {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void recordScoreSubmittedPersistsCorrectEventType() {
        auditService.recordScoreSubmitted("Alice", 100, "EASY", "abc123hash");

        AuditEvent event = capturedEvent();
        assertThat(event.getEventType()).isEqualTo("SCORE_SUBMITTED");
    }

    @Test
    void recordScoreSubmittedIncludesAllRequiredFields() {
        auditService.recordScoreSubmitted("Bob", 250, "HARD", "sha256hash");

        AuditEvent event = capturedEvent();
        assertThat(event.getNickname()).isEqualTo("Bob");
        assertThat(event.getScore()).isEqualTo(250);
        assertThat(event.getDifficulty()).isEqualTo("HARD");
        assertThat(event.getClientIpHash()).isEqualTo("sha256hash");
    }

    @Test
    void recordScoreSubmittedNeverStoresRawNickname() {
        auditService.recordScoreSubmitted("Carol", 50, "MEDIUM", "hashed-ip");

        AuditEvent event = capturedEvent();
        assertThat(event.getClientIpHash()).isEqualTo("hashed-ip");
        assertThat(event.getClientIpHash()).doesNotContain(".");
    }

    @Test
    void recordApplicationStartedPersistsCorrectEventType() {
        auditService.recordApplicationStarted();

        AuditEvent event = capturedEvent();
        assertThat(event.getEventType()).isEqualTo("APPLICATION_STARTED");
        assertThat(event.getNickname()).isNull();
        assertThat(event.getScore()).isNull();
    }

    @Test
    void recordApplicationStoppedPersistsCorrectEventType() {
        auditService.recordApplicationStopped();

        AuditEvent event = capturedEvent();
        assertThat(event.getEventType()).isEqualTo("APPLICATION_STOPPED");
        assertThat(event.getNickname()).isNull();
        assertThat(event.getScore()).isNull();
    }
}
