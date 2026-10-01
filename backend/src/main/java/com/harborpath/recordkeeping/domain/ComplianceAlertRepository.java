package com.harborpath.recordkeeping.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceAlertRepository extends JpaRepository<ComplianceAlert, UUID> {
	List<ComplianceAlert> findByPlanIdOrderByStatusAscCodeAsc(UUID planId);

	Optional<ComplianceAlert> findByPlanIdAndParticipantIdAndCode(UUID planId, UUID participantId, String code);
}
