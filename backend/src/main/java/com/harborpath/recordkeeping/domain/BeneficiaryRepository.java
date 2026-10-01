package com.harborpath.recordkeeping.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {
	List<Beneficiary> findByAccountId(UUID accountId);

	List<Beneficiary> findByAccountIdIn(List<UUID> accountIds);

	void deleteByAccountId(UUID accountId);
}
