package com.harborpath.recordkeeping.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "beneficiary")
public class Beneficiary {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID accountId;

	@Column(nullable = false)
	private String fullName;

	@Column(nullable = false)
	private String relationship;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal percent;

	@Column(nullable = false)
	private boolean primaryFlag;

	protected Beneficiary() {
	}

	public Beneficiary(UUID id, UUID accountId, String fullName, String relationship, BigDecimal percent,
			boolean primaryFlag) {
		this.id = id;
		this.accountId = accountId;
		this.fullName = fullName;
		this.relationship = relationship;
		this.percent = percent;
		this.primaryFlag = primaryFlag;
	}

	public UUID getId() {
		return id;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public String getFullName() {
		return fullName;
	}

	public String getRelationship() {
		return relationship;
	}

	public BigDecimal getPercent() {
		return percent;
	}

	public boolean isPrimaryFlag() {
		return primaryFlag;
	}
}
