package com.snakeforged.audit;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Writes audit events at application startup and shutdown so operators
 * have a complete lifecycle trail in the AUDIT_EVENT table.
 */
@Component
public class ApplicationLifecycleListener {

    private final AuditService auditService;

    public ApplicationLifecycleListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        auditService.recordApplicationStarted();
    }

    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        auditService.recordApplicationStopped();
    }
}
