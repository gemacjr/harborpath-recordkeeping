package com.harborpath.recordkeeping.domain;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plan_participant")
public class PlanParticipant {

	@Id
	private UUID id;

	private UUID userId;

	@Column(nullable = false)
	private UUID planId;

	@Column(nullable = false)
	private String fullName;

	private LocalDate hireDate;

	protected PlanParticipant() {
	}

	public PlanParticipant(UUID id, UUID userId, UUID planId, String fullName, LocalDate hireDate) {
		this.id = id;
		this.userId = userId;
		this.planId = planId;
		this.fullName = fullName;
		this.hireDate = hireDate;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getPlanId() {
		return planId;
	}

	public String getFullName() {
		return fullName;
	}

	public LocalDate getHireDate() {
		return hireDate;
	}
}
