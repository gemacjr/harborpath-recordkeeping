package com.harborpath.recordkeeping.record;

import com.harborpath.recordkeeping.domain.AlertStatus;

final class BeneficiaryCompliance {
	static final String MISSING_PRIMARY = "MISSING_PRIMARY_BENEFICIARY";

	enum Decision {
		OPEN, REOPEN, CLEAR, LEAVE
	}

	private BeneficiaryCompliance() {
	}

	static Decision decide(boolean hasPrimary, AlertStatus current) {
		if (!hasPrimary) {
			if (current == null) {
				return Decision.OPEN;
			}
			if (current == AlertStatus.CLEARED) {
				return Decision.REOPEN;
			}
			return Decision.LEAVE;
		}
		if (current == AlertStatus.OPEN || current == AlertStatus.ACKNOWLEDGED) {
			return Decision.CLEAR;
		}
		return Decision.LEAVE;
	}
}
