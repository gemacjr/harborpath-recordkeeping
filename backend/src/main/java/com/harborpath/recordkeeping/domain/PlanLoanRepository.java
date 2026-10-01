package com.harborpath.recordkeeping.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanLoanRepository extends JpaRepository<PlanLoan, UUID> {
	List<PlanLoan> findByParticipantIdOrderByRequestedAtDesc(UUID participantId);

	List<PlanLoan> findByPlanIdOrderByRequestedAtDesc(UUID planId);

	boolean existsByAccountIdAndStatus(UUID accountId, LoanStatus status);
}
