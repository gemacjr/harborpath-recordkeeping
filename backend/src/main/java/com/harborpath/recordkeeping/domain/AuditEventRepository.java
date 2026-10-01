package com.harborpath.recordkeeping.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
	List<AuditEvent> findByPlanIdOrderByOccurredAtDesc(UUID planId);

	List<AuditEvent> findByActorUserIdOrderByOccurredAtDesc(UUID actorUserId);
}
