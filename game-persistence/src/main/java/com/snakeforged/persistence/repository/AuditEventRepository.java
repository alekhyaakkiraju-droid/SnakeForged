package com.snakeforged.persistence.repository;

import com.snakeforged.persistence.entity.AuditEvent;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Insert-and-read-only repository for audit events.
 * Intentionally extends the base {@link Repository} marker rather than
 * {@code JpaRepository} so that no delete or bulk-update methods are
 * available to application code, enforcing audit-log immutability.
 */
@org.springframework.stereotype.Repository
public interface AuditEventRepository extends Repository<AuditEvent, Long> {

    /** Persist a new audit event. Never call with a pre-existing ID. */
    AuditEvent save(AuditEvent entity);

    List<AuditEvent> findAll();

    Optional<AuditEvent> findById(Long id);

    long count();
}
