package com.harborpath.recordkeeping.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "savings_plan")
public class SavingsPlan {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private int planYear;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal deferralCapPercent;

	@Column(nullable = false)
	private UUID sponsorUserId;

	protected SavingsPlan() {
	}

	public SavingsPlan(UUID id, String name, int planYear, BigDecimal deferralCapPercent, UUID sponsorUserId) {
		this.id = id;
		this.name = name;
		this.planYear = planYear;
		this.deferralCapPercent = deferralCapPercent;
		this.sponsorUserId = sponsorUserId;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getPlanYear() {
		return planYear;
	}

	public BigDecimal getDeferralCapPercent() {
		return deferralCapPercent;
	}

	public UUID getSponsorUserId() {
		return sponsorUserId;
	}
}
