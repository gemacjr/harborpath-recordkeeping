package com.harborpath.recordkeeping.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsPlanRepository extends JpaRepository<SavingsPlan, UUID> {
	List<SavingsPlan> findBySponsorUserId(UUID sponsorUserId);
}
