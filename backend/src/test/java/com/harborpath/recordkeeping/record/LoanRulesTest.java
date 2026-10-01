package com.harborpath.recordkeeping.record;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class LoanRulesTest {

	@Test
	void maxAmountIsLesserOfHalfBalanceAndIrsCap() {
		assertThat(LoanRules.maxAmount(new BigDecimal("184220.16"))).isEqualByComparingTo("50000.00");
		assertThat(LoanRules.maxAmount(new BigDecimal("20000.00"))).isEqualByComparingTo("10000.00");
	}

	@Test
	void fiveThousandIsWithinMayaBalance() {
		assertThat(LoanRules.exceedsCap(new BigDecimal("5000"), new BigDecimal("184220.16"))).isFalse();
	}

	@Test
	void amountOverIrsCapIsRejectedEvenWhenHalfBalanceIsHigher() {
		assertThat(LoanRules.exceedsCap(new BigDecimal("50000.01"), new BigDecimal("184220.16"))).isTrue();
	}

	@Test
	void zeroAndNegativeAmountsAreInvalid() {
		assertThat(LoanRules.isPositive(BigDecimal.ZERO)).isFalse();
		assertThat(LoanRules.isPositive(new BigDecimal("-1"))).isFalse();
		assertThat(LoanRules.isPositive(new BigDecimal("1.00"))).isTrue();
	}
}
