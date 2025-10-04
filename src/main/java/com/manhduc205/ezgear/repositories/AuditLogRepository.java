package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    AuditLog findTopByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
}
