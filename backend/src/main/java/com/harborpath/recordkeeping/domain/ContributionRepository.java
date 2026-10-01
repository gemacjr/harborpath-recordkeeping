package com.harborpath.recordkeeping.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributionRepository extends JpaRepository<Contribution, UUID> {
	List<Contribution> findByParticipantIdOrderByPostedAtDesc(UUID participantId);

	List<Contribution> findByPlanIdOrderByPostedAtDesc(UUID planId);
}
