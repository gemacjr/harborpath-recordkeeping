package com.harborpath.recordkeeping.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plan_loan")
public class PlanLoan {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID accountId;

	@Column(nullable = false)
	private UUID participantId;

	@Column(nullable = false)
	private UUID planId;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LoanStatus status;

	@Column(nullable = false)
	private Instant requestedAt;

	private Instant decidedAt;

	protected PlanLoan() {
	}

	public PlanLoan(UUID id, UUID accountId, UUID participantId, UUID planId, BigDecimal amount, LoanStatus status,
			Instant requestedAt) {
		this.id = id;
		this.accountId = accountId;
		this.participantId = participantId;
		this.planId = planId;
		this.amount = amount;
		this.status = status;
		this.requestedAt = requestedAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public UUID getParticipantId() {
		return participantId;
	}

	public UUID getPlanId() {
		return planId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public LoanStatus getStatus() {
		return status;
	}

	public Instant getRequestedAt() {
		return requestedAt;
	}

	public Instant getDecidedAt() {
		return decidedAt;
	}

	public void deny(Instant at) {
		this.status = LoanStatus.DENIED;
		this.decidedAt = at;
	}

	public void disburse(Instant at) {
		this.status = LoanStatus.DISBURSED;
		this.decidedAt = at;
	}
}
