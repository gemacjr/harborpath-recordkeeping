package com.harborpath.recordkeeping.record;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.harborpath.recordkeeping.domain.AlertStatus;

class BeneficiaryComplianceTest {

	@Test
	void missingPrimaryOpensWhenNoAlertExists() {
		assertThat(BeneficiaryCompliance.decide(false, null)).isEqualTo(BeneficiaryCompliance.Decision.OPEN);
	}

	@Test
	void missingPrimaryReopensAfterCleared() {
		assertThat(BeneficiaryCompliance.decide(false, AlertStatus.CLEARED))
				.isEqualTo(BeneficiaryCompliance.Decision.REOPEN);
	}

	@Test
	void missingPrimaryLeavesAcknowledgedQueueItem() {
		assertThat(BeneficiaryCompliance.decide(false, AlertStatus.ACKNOWLEDGED))
				.isEqualTo(BeneficiaryCompliance.Decision.LEAVE);
	}

	@Test
	void restoringPrimaryClearsOpenOrAcknowledged() {
		assertThat(BeneficiaryCompliance.decide(true, AlertStatus.OPEN))
				.isEqualTo(BeneficiaryCompliance.Decision.CLEAR);
		assertThat(BeneficiaryCompliance.decide(true, AlertStatus.ACKNOWLEDGED))
				.isEqualTo(BeneficiaryCompliance.Decision.CLEAR);
	}

	@Test
	void completeDesignationDoesNothingWhenAlreadyCleared() {
		assertThat(BeneficiaryCompliance.decide(true, AlertStatus.CLEARED))
				.isEqualTo(BeneficiaryCompliance.Decision.LEAVE);
		assertThat(BeneficiaryCompliance.decide(true, null)).isEqualTo(BeneficiaryCompliance.Decision.LEAVE);
	}
}
