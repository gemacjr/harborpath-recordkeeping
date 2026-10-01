package com.harborpath.recordkeeping.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "compliance_alert")
public class ComplianceAlert {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID planId;

	@Column(nullable = false)
	private UUID participantId;

	@Column(nullable = false)
	private String code;

	@Column(nullable = false)
	private String message;

	@Column(nullable = false)
	private String severity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AlertStatus status;

	protected ComplianceAlert() {
	}

	public ComplianceAlert(UUID id, UUID planId, UUID participantId, String code, String message, String severity,
			AlertStatus status) {
		this.id = id;
		this.planId = planId;
		this.participantId = participantId;
		this.code = code;
		this.message = message;
		this.severity = severity;
		this.status = status;
	}

	public UUID getId() {
		return id;
	}

	public UUID getPlanId() {
		return planId;
	}

	public UUID getParticipantId() {
		return participantId;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public String getSeverity() {
		return severity;
	}

	public AlertStatus getStatus() {
		return status;
	}

	public void setStatus(AlertStatus status) {
		this.status = status;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
