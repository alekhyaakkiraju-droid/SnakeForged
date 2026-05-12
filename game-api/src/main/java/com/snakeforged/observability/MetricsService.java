package com.snakeforged.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Central holder for application-level Micrometer counters.
 * Injected wherever score submission success or rejection needs recording.
 */
@Component
public class MetricsService {

    private final Counter submissionsTotal;
    private final Counter submissionsRejected;

    public MetricsService(MeterRegistry registry) {
        this.submissionsTotal = Counter.builder("snakeweb_highscore_submissions_total")
                .description("Total successful highscore submissions")
                .register(registry);
        this.submissionsRejected = Counter.builder("snakeweb_highscore_submissions_rejected_total")
                .description("Total rejected highscore submissions (validation or business-rule failures)")
                .register(registry);
    }

    /** Increment on every successful score persistence. */
    public void recordSubmission() {
        submissionsTotal.increment();
    }

    /** Increment on every rejected score attempt (validation or plausibility failure). */
    public void recordRejection() {
        submissionsRejected.increment();
    }
}
