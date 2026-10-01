package com.harborpath.recordkeeping.record;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class LoanRules {
	static final BigDecimal IRS_CAP = new BigDecimal("50000.00");

	private LoanRules() {
	}

	static boolean isPositive(BigDecimal amount) {
		return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
	}

	static BigDecimal maxAmount(BigDecimal balance) {
		BigDecimal half = balance.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
		return half.min(IRS_CAP);
	}

	static boolean exceedsCap(BigDecimal amount, BigDecimal balance) {
		return amount.compareTo(maxAmount(balance)) > 0;
	}
}
