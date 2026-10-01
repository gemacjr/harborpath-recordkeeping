package com.harborpath.recordkeeping.record;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.harborpath.recordkeeping.domain.AccountType;
import com.harborpath.recordkeeping.domain.AlertStatus;

record HouseholdResponse(String participantName, List<AccountSummary> accounts, TapeTotals tape) {
}

record TapeTotals(BigDecimal ytdEmployee, BigDecimal ytdMatch) {
}

record AccountSummary(UUID id, AccountType type, String name, BigDecimal balance, BigDecimal deferralPercent,
		BigDecimal ytdEmployee, BigDecimal ytdMatch, boolean beneficiaryComplete) {
}

record BeneficiaryResponse(UUID id, String fullName, String relationship, BigDecimal percent, boolean primaryFlag) {
}

record BeneficiaryWrite(String fullName, String relationship, BigDecimal percent, boolean primaryFlag) {
}

record PlanSummary(UUID id, String name, int planYear, int participantCount, long openAlertCount,
		BigDecimal deferralCapPercent) {
}

record CensusRow(UUID participantId, String fullName, BigDecimal deferralPercent, BigDecimal balance,
		boolean beneficiaryComplete) {
}

record AlertResponse(UUID id, UUID planId, UUID participantId, String code, String message, String severity,
		AlertStatus status) {
}

record DeferralWrite(BigDecimal percent) {
}

record AlertStatusWrite(AlertStatus status) {
}

record ContributionRow(UUID id, UUID accountId, UUID participantId, UUID planId, String participantName,
		com.harborpath.recordkeeping.domain.ContributionKind kind, BigDecimal amount, java.time.Instant postedAt,
		String memo, String accountName) {
}

record AmountWrite(BigDecimal amount) {
}

record WithdrawalWrite(BigDecimal amount, String memo) {
}

record PayrollWrite(BigDecimal periodPay) {
}

record PayrollResult(int accountsPosted, BigDecimal employeeTotal, BigDecimal matchTotal) {
}

record AuditRow(UUID id, java.time.Instant occurredAt, String actorUsername,
		com.harborpath.recordkeeping.domain.Role actorRole, UUID planId, UUID participantId,
		com.harborpath.recordkeeping.domain.AuditAction action, String summary) {
}

record LoanRow(UUID id, UUID accountId, UUID participantId, UUID planId, String participantName, String accountName,
		java.math.BigDecimal amount, com.harborpath.recordkeeping.domain.LoanStatus status,
		java.time.Instant requestedAt, java.time.Instant decidedAt) {
}

record LoanDecisionWrite(LoanDecision decision) {
}

enum LoanDecision {
	APPROVE, DENY
}
