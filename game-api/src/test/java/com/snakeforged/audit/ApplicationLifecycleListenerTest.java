package com.snakeforged.audit;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ApplicationLifecycleListenerTest {

    @Test
    void onApplicationReadyCallsRecordApplicationStarted() {
        AuditService auditService = mock(AuditService.class);
        ApplicationLifecycleListener listener = new ApplicationLifecycleListener(auditService);

        listener.onApplicationReady();

        verify(auditService).recordApplicationStarted();
    }

    @Test
    void onContextClosedCallsRecordApplicationStopped() {
        AuditService auditService = mock(AuditService.class);
        ApplicationLifecycleListener listener = new ApplicationLifecycleListener(auditService);

        listener.onContextClosed();

        verify(auditService).recordApplicationStopped();
    }
}
