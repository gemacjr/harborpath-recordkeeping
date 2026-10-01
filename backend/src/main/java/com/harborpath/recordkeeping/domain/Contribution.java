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
@Table(name = "contribution")
public class Contribution {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID accountId;

	@Column(nullable = false)
	private UUID participantId;

	@Column(nullable = false)
	private UUID planId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ContributionKind kind;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false)
	private Instant postedAt;

	@Column(nullable = false)
	private String memo;

	protected Contribution() {
	}

	public Contribution(UUID id, UUID accountId, UUID participantId, UUID planId, ContributionKind kind,
			BigDecimal amount, Instant postedAt, String memo) {
		this.id = id;
		this.accountId = accountId;
		this.participantId = participantId;
		this.planId = planId;
		this.kind = kind;
		this.amount = amount;
		this.postedAt = postedAt;
		this.memo = memo;
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

	public ContributionKind getKind() {
		return kind;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Instant getPostedAt() {
		return postedAt;
	}

	public String getMemo() {
		return memo;
	}
}
