package com.harborpath.recordkeeping.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanParticipantRepository extends JpaRepository<PlanParticipant, UUID> {
	Optional<PlanParticipant> findByUserId(UUID userId);

	List<PlanParticipant> findByPlanId(UUID planId);
}
