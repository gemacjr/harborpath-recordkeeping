package com.harborpath.recordkeeping.record;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.harborpath.recordkeeping.auth.AuthenticatedUser;
import com.harborpath.recordkeeping.auth.CurrentUser;
import com.harborpath.recordkeeping.domain.AuditAction;
import com.harborpath.recordkeeping.domain.AuditEvent;
import com.harborpath.recordkeeping.domain.AuditEventRepository;

@Component
class AuditRecorder {
	private final CurrentUser currentUser;
	private final AuditEventRepository events;

	AuditRecorder(CurrentUser currentUser, AuditEventRepository events) {
		this.currentUser = currentUser;
		this.events = events;
	}

	void record(AuditAction action, UUID planId, UUID participantId, String summary) {
		AuthenticatedUser actor = currentUser.require();
		events.save(new AuditEvent(UUID.randomUUID(), Instant.now(), actor.id(), actor.username(), actor.role(), planId,
				participantId, action, summary));
	}
}
