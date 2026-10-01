package com.harborpath.recordkeeping.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

	@Id
	private UUID id;

	@Column(nullable = false)
	private Instant occurredAt;

	@Column(nullable = false)
	private UUID actorUserId;

	@Column(nullable = false)
	private String actorUsername;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role actorRole;

	@Column(nullable = false)
	private UUID planId;

	private UUID participantId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuditAction action;

	@Column(nullable = false, length = 512)
	private String summary;

	protected AuditEvent() {
	}

	public AuditEvent(UUID id, Instant occurredAt, UUID actorUserId, String actorUsername, Role actorRole, UUID planId,
			UUID participantId, AuditAction action, String summary) {
		this.id = id;
		this.occurredAt = occurredAt;
		this.actorUserId = actorUserId;
		this.actorUsername = actorUsername;
		this.actorRole = actorRole;
		this.planId = planId;
		this.participantId = participantId;
		this.action = action;
		this.summary = summary;
	}

	public UUID getId() {
		return id;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public UUID getActorUserId() {
		return actorUserId;
	}

	public String getActorUsername() {
		return actorUsername;
	}

	public Role getActorRole() {
		return actorRole;
	}

	public UUID getPlanId() {
		return planId;
	}

	public UUID getParticipantId() {
		return participantId;
	}

	public AuditAction getAction() {
		return action;
	}

	public String getSummary() {
		return summary;
	}
}
