package com.harborpath.recordkeeping.record;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harborpath.recordkeeping.auth.AuthenticatedUser;
import com.harborpath.recordkeeping.auth.CurrentUser;
import com.harborpath.recordkeeping.domain.Account;
import com.harborpath.recordkeeping.domain.AccountRepository;
import com.harborpath.recordkeeping.domain.AccountType;
import com.harborpath.recordkeeping.domain.AlertStatus;
import com.harborpath.recordkeeping.domain.AuditAction;
import com.harborpath.recordkeeping.domain.AuditEvent;
import com.harborpath.recordkeeping.domain.AuditEventRepository;
import com.harborpath.recordkeeping.domain.Beneficiary;
import com.harborpath.recordkeeping.domain.BeneficiaryRepository;
import com.harborpath.recordkeeping.domain.ComplianceAlert;
import com.harborpath.recordkeeping.domain.ComplianceAlertRepository;
import com.harborpath.recordkeeping.domain.Contribution;
import com.harborpath.recordkeeping.domain.ContributionKind;
import com.harborpath.recordkeeping.domain.ContributionRepository;
import com.harborpath.recordkeeping.domain.LoanStatus;
import com.harborpath.recordkeeping.domain.PlanLoan;
import com.harborpath.recordkeeping.domain.PlanLoanRepository;
import com.harborpath.recordkeeping.domain.PlanParticipant;
import com.harborpath.recordkeeping.domain.PlanParticipantRepository;
import com.harborpath.recordkeeping.domain.Role;
import com.harborpath.recordkeeping.domain.SavingsPlan;
import com.harborpath.recordkeeping.domain.SavingsPlanRepository;
import com.harborpath.recordkeeping.web.ApiException;

@Service
public class RecordkeepingService {
	private final CurrentUser currentUser;
	private final PlanParticipantRepository participants;
	private final AccountRepository accounts;
	private final BeneficiaryRepository beneficiaries;
	private final SavingsPlanRepository plans;
	private final ComplianceAlertRepository alerts;
	private final ContributionRepository contributions;
	private final AuditRecorder audit;
	private final AuditEventRepository auditEvents;
	private final PlanLoanRepository loans;

	public RecordkeepingService(CurrentUser currentUser, PlanParticipantRepository participants,
			AccountRepository accounts, BeneficiaryRepository beneficiaries, SavingsPlanRepository plans,
			ComplianceAlertRepository alerts, ContributionRepository contributions, AuditRecorder audit,
			AuditEventRepository auditEvents, PlanLoanRepository loans) {
		this.currentUser = currentUser;
		this.participants = participants;
		this.accounts = accounts;
		this.beneficiaries = beneficiaries;
		this.plans = plans;
		this.alerts = alerts;
		this.contributions = contributions;
		this.audit = audit;
		this.auditEvents = auditEvents;
		this.loans = loans;
	}

	public HouseholdResponse household() {
		PlanParticipant participant = requireOwnParticipant();
		List<Account> owned = accounts.findByParticipantId(participant.getId());
		BigDecimal ytdEmployee = owned.stream().map(Account::getYtdEmployee).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal ytdMatch = owned.stream().map(Account::getYtdMatch).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new HouseholdResponse(participant.getFullName(), owned.stream().map(this::toSummary).toList(),
				new TapeTotals(ytdEmployee, ytdMatch));
	}

	public AccountSummary account(UUID accountId) {
		return toSummary(requireOwnedAccount(accountId));
	}

	@Transactional
	public AccountSummary updateDeferral(UUID accountId, BigDecimal percent) {
		Account account = requireOwnedAccount(accountId);
		if (account.getType() != AccountType.RETIREMENT_401K) {
			throw ApiException.unprocessable("percent", "Deferral applies only to 401(k) accounts.");
		}
		SavingsPlan plan = plans.findById(ownedParticipant(account.getParticipantId()).getPlanId())
				.orElseThrow(() -> ApiException.notFound("Plan not found."));
		if (percent.compareTo(BigDecimal.ZERO) < 0 || percent.compareTo(plan.getDeferralCapPercent()) > 0) {
			throw ApiException.unprocessable("percent",
					"Deferral cannot exceed the plan cap of " + plan.getDeferralCapPercent().stripTrailingZeros().toPlainString() + "%.");
		}
		BigDecimal previous = account.getDeferralPercent();
		account.setDeferralPercent(percent);
		Account saved = accounts.save(account);
		PlanParticipant participant = ownedParticipant(account.getParticipantId());
		audit.record(AuditAction.DEFERRAL_CHANGED, participant.getPlanId(), participant.getId(),
				"Deferral " + formatPercent(previous) + "% → " + formatPercent(percent) + "%");
		return toSummary(saved);
	}

	public List<BeneficiaryResponse> beneficiaries(UUID accountId) {
		requireOwnedAccount(accountId);
		return beneficiaries.findByAccountId(accountId).stream().map(this::toBeneficiary).toList();
	}

	@Transactional
	public List<BeneficiaryResponse> replaceBeneficiaries(UUID accountId, List<BeneficiaryWrite> writes) {
		Account account = requireOwnedAccount(accountId);
		BigDecimal total = writes.stream().map(BeneficiaryWrite::percent).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (total.compareTo(new BigDecimal("100")) != 0) {
			throw ApiException.unprocessable("percent", "Beneficiary percents must sum to 100.");
		}
		beneficiaries.deleteByAccountId(accountId);
		List<Beneficiary> saved = writes.stream()
				.map(write -> new Beneficiary(UUID.randomUUID(), accountId, write.fullName(), write.relationship(),
						write.percent(), write.primaryFlag()))
				.map(beneficiaries::save)
				.toList();
		PlanParticipant participant = ownedParticipant(account.getParticipantId());
		audit.record(AuditAction.BENEFICIARY_REPLACED, participant.getPlanId(), participant.getId(),
				"Replaced designations on " + account.getName());
		syncRetirementBeneficiaryAlert(account, participant);
		return saved.stream().map(this::toBeneficiary).toList();
	}

	public List<PlanSummary> plans() {
		AuthenticatedUser user = currentUser.requireRole(Role.SPONSOR);
		return plans.findBySponsorUserId(user.id()).stream().map(plan -> {
			int count = participants.findByPlanId(plan.getId()).size();
			long openAlerts = alerts.findByPlanIdOrderByStatusAscCodeAsc(plan.getId()).stream()
					.filter(alert -> alert.getStatus() == AlertStatus.OPEN)
					.count();
			return new PlanSummary(plan.getId(), plan.getName(), plan.getPlanYear(), count, openAlerts,
					plan.getDeferralCapPercent());
		}).toList();
	}

	public List<CensusRow> census(UUID planId) {
		SavingsPlan plan = requireSponsoredPlan(planId);
		List<PlanParticipant> rows = participants.findByPlanId(plan.getId());
		if (rows.isEmpty()) {
			return List.of();
		}
		List<UUID> ids = rows.stream().map(PlanParticipant::getId).toList();
		Map<UUID, List<Account>> byParticipant = accounts.findByParticipantIdIn(ids).stream()
				.collect(Collectors.groupingBy(Account::getParticipantId));
		List<UUID> accountIds = byParticipant.values().stream().flatMap(List::stream).map(Account::getId).toList();
		Map<UUID, List<Beneficiary>> byAccount = accountIds.isEmpty()
				? Map.of()
				: beneficiaries.findByAccountIdIn(accountIds).stream().collect(Collectors.groupingBy(Beneficiary::getAccountId));
		return rows.stream().map(row -> {
			List<Account> owned = byParticipant.getOrDefault(row.getId(), List.of());
			Account retirement = owned.stream().filter(a -> a.getType() == AccountType.RETIREMENT_401K).findFirst()
					.orElse(null);
			BigDecimal deferral = retirement == null ? null : retirement.getDeferralPercent();
			BigDecimal balance = owned.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
			boolean complete = retirement != null && byAccount.getOrDefault(retirement.getId(), List.of()).stream()
					.anyMatch(Beneficiary::isPrimaryFlag);
			return new CensusRow(row.getId(), row.getFullName(), deferral, balance, complete);
		}).toList();
	}

	public List<AlertResponse> alerts(UUID planId) {
		requireSponsoredPlan(planId);
		return alerts.findByPlanIdOrderByStatusAscCodeAsc(planId).stream().map(this::toAlert).toList();
	}

	@Transactional
	public AlertResponse acknowledge(UUID alertId) {
		currentUser.requireRole(Role.SPONSOR);
		ComplianceAlert alert = alerts.findById(alertId).orElseThrow(() -> ApiException.notFound("Alert not found."));
		requireSponsoredPlan(alert.getPlanId());
		alert.setStatus(AlertStatus.ACKNOWLEDGED);
		AlertResponse saved = toAlert(alerts.save(alert));
		audit.record(AuditAction.ALERT_ACKNOWLEDGED, alert.getPlanId(), alert.getParticipantId(),
				"Acknowledged " + alert.getCode());
		return saved;
	}

	public List<ContributionRow> myContributions() {
		PlanParticipant participant = requireOwnParticipant();
		return contributions.findByParticipantIdOrderByPostedAtDesc(participant.getId()).stream()
				.map(row -> toContribution(row, participant.getFullName()))
				.toList();
	}

	public List<ContributionRow> planContributions(UUID planId) {
		requireSponsoredPlan(planId);
		Map<UUID, String> names = participants.findByPlanId(planId).stream()
				.collect(Collectors.toMap(PlanParticipant::getId, PlanParticipant::getFullName));
		return contributions.findByPlanIdOrderByPostedAtDesc(planId).stream()
				.map(row -> toContribution(row, names.getOrDefault(row.getParticipantId(), "Participant")))
				.toList();
	}

	@Transactional
	public ContributionRow contributeEducation(UUID accountId, BigDecimal amount) {
		Account account = requireOwnedAccount(accountId);
		if (account.getType() != AccountType.EDUCATION_529) {
			throw ApiException.unprocessable("amount", "One-time contributions apply only to 529 accounts.");
		}
		requirePositive(amount);
		account.credit(amount);
		accounts.save(account);
		PlanParticipant participant = ownedParticipant(account.getParticipantId());
		Contribution saved = contributions.save(new Contribution(UUID.randomUUID(), account.getId(),
				participant.getId(), participant.getPlanId(), ContributionKind.EDUCATION_529, amount, Instant.now(),
				"One-time 529 contribution"));
		audit.record(AuditAction.EDUCATION_CONTRIBUTED, participant.getPlanId(), participant.getId(),
				"529 contribution " + amount.toPlainString());
		return toContribution(saved, participant.getFullName());
	}

	@Transactional
	public ContributionRow withdrawEducation(UUID accountId, BigDecimal amount, String memo) {
		Account account = requireOwnedAccount(accountId);
		if (account.getType() != AccountType.EDUCATION_529) {
			throw ApiException.unprocessable("amount", "Qualified withdrawals apply only to 529 accounts.");
		}
		requirePositive(amount);
		if (amount.compareTo(account.getBalance()) > 0) {
			throw ApiException.unprocessable("amount", "Withdrawal cannot exceed the 529 balance.");
		}
		account.debit(amount);
		accounts.save(account);
		PlanParticipant participant = ownedParticipant(account.getParticipantId());
		String note = (memo == null || memo.isBlank()) ? "Qualified education withdrawal" : memo.trim();
		Contribution saved = contributions.save(new Contribution(UUID.randomUUID(), account.getId(),
				participant.getId(), participant.getPlanId(), ContributionKind.QUALIFIED_WITHDRAWAL, amount.negate(),
				Instant.now(), note));
		audit.record(AuditAction.EDUCATION_WITHDRAWN, participant.getPlanId(), participant.getId(),
				"529 withdrawal " + amount.toPlainString());
		return toContribution(saved, participant.getFullName());
	}

	@Transactional
	public PayrollResult postPayroll(UUID planId, BigDecimal periodPay) {
		SavingsPlan plan = requireSponsoredPlan(planId);
		requirePositive(periodPay);
		List<PlanParticipant> rows = participants.findByPlanId(plan.getId());
		BigDecimal employeeTotal = BigDecimal.ZERO;
		BigDecimal matchTotal = BigDecimal.ZERO;
		int posted = 0;
		Instant now = Instant.now();
		for (PlanParticipant row : rows) {
			Account retirement = accounts.findByParticipantId(row.getId()).stream()
					.filter(account -> account.getType() == AccountType.RETIREMENT_401K)
					.findFirst()
					.orElse(null);
			if (retirement == null || retirement.getDeferralPercent() == null) {
				continue;
			}
			BigDecimal employee = periodPay.multiply(retirement.getDeferralPercent())
					.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
			BigDecimal match = employee.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
			if (employee.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			retirement.credit(employee.add(match));
			retirement.addYtdEmployee(employee);
			retirement.addYtdMatch(match);
			accounts.save(retirement);
			contributions.save(new Contribution(UUID.randomUUID(), retirement.getId(), row.getId(), plan.getId(),
					ContributionKind.PAYROLL_EMPLOYEE, employee, now,
					"Payroll deferral at " + retirement.getDeferralPercent().stripTrailingZeros().toPlainString() + "%"));
			contributions.save(new Contribution(UUID.randomUUID(), retirement.getId(), row.getId(), plan.getId(),
					ContributionKind.PAYROLL_MATCH, match, now, "Employer match 50% of deferral"));
			employeeTotal = employeeTotal.add(employee);
			matchTotal = matchTotal.add(match);
			posted++;
		}
		audit.record(AuditAction.PAYROLL_POSTED, plan.getId(), null,
				"Payroll posted for " + posted + " accounts");
		return new PayrollResult(posted, employeeTotal, matchTotal);
	}

	@Transactional
	public LoanRow requestLoan(UUID accountId, BigDecimal amount) {
		Account account = requireOwnedAccount(accountId);
		if (account.getType() != AccountType.RETIREMENT_401K) {
			throw ApiException.unprocessable("amount", "Plan loans apply only to 401(k) accounts.");
		}
		if (!LoanRules.isPositive(amount)) {
			throw ApiException.unprocessable("amount", "Amount must be greater than zero.");
		}
		if (LoanRules.exceedsCap(amount, account.getBalance())) {
			throw ApiException.unprocessable("amount",
					"Loan cannot exceed the lesser of 50% of the 401(k) balance or $50,000.");
		}
		if (loans.existsByAccountIdAndStatus(account.getId(), LoanStatus.REQUESTED)) {
			throw ApiException.unprocessable("amount", "A loan request is already on file for this account.");
		}
		PlanParticipant participant = ownedParticipant(account.getParticipantId());
		PlanLoan saved = loans.save(new PlanLoan(UUID.randomUUID(), account.getId(), participant.getId(),
				participant.getPlanId(), amount, LoanStatus.REQUESTED, Instant.now()));
		audit.record(AuditAction.LOAN_REQUESTED, participant.getPlanId(), participant.getId(),
				"Loan request " + amount.toPlainString());
		return toLoan(saved, participant.getFullName(), account.getName());
	}

	public List<LoanRow> myLoans() {
		PlanParticipant participant = requireOwnParticipant();
		return loans.findByParticipantIdOrderByRequestedAtDesc(participant.getId()).stream()
				.map(loan -> toLoan(loan, participant.getFullName(),
						accounts.findById(loan.getAccountId()).map(Account::getName).orElse("Account")))
				.toList();
	}

	public List<LoanRow> planLoans(UUID planId) {
		requireSponsoredPlan(planId);
		Map<UUID, String> names = participants.findByPlanId(planId).stream()
				.collect(Collectors.toMap(PlanParticipant::getId, PlanParticipant::getFullName));
		return loans.findByPlanIdOrderByRequestedAtDesc(planId).stream()
				.map(loan -> toLoan(loan, names.getOrDefault(loan.getParticipantId(), "Participant"),
						accounts.findById(loan.getAccountId()).map(Account::getName).orElse("Account")))
				.toList();
	}

	@Transactional
	public LoanRow decideLoan(UUID loanId, LoanDecision decision) {
		currentUser.requireRole(Role.SPONSOR);
		PlanLoan loan = loans.findById(loanId).orElseThrow(() -> ApiException.notFound("Loan not found."));
		requireSponsoredPlan(loan.getPlanId());
		if (loan.getStatus() != LoanStatus.REQUESTED) {
			throw ApiException.unprocessable("decision", "Only a requested loan can be decided.");
		}
		if (decision == null) {
			throw ApiException.unprocessable("decision", "Decision must be APPROVE or DENY.");
		}
		Instant now = Instant.now();
		PlanParticipant participant = ownedParticipant(loan.getParticipantId());
		Account account = accounts.findById(loan.getAccountId())
				.orElseThrow(() -> ApiException.notFound("Account not found."));
		if (decision == LoanDecision.DENY) {
			loan.deny(now);
			loans.save(loan);
			audit.record(AuditAction.LOAN_DENIED, loan.getPlanId(), loan.getParticipantId(),
					"Denied loan " + loan.getAmount().toPlainString());
			return toLoan(loan, participant.getFullName(), account.getName());
		}
		if (loan.getAmount().compareTo(account.getBalance()) > 0) {
			throw ApiException.unprocessable("amount", "Disbursement cannot exceed the current 401(k) balance.");
		}
		account.debit(loan.getAmount());
		accounts.save(account);
		loan.disburse(now);
		loans.save(loan);
		contributions.save(new Contribution(UUID.randomUUID(), account.getId(), participant.getId(), loan.getPlanId(),
				ContributionKind.LOAN_DISBURSEMENT, loan.getAmount().negate(), now, "401(k) loan disbursement"));
		audit.record(AuditAction.LOAN_APPROVED, loan.getPlanId(), loan.getParticipantId(),
				"Disbursed loan " + loan.getAmount().toPlainString());
		return toLoan(loan, participant.getFullName(), account.getName());
	}

	public List<AuditRow> myAudit() {
		AuthenticatedUser user = currentUser.require();
		return auditEvents.findByActorUserIdOrderByOccurredAtDesc(user.id()).stream().map(this::toAudit).toList();
	}

	public List<AuditRow> planAudit(UUID planId) {
		requireSponsoredPlan(planId);
		return auditEvents.findByPlanIdOrderByOccurredAtDesc(planId).stream().map(this::toAudit).toList();
	}

	private void requirePositive(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw ApiException.unprocessable("amount", "Amount must be greater than zero.");
		}
	}

	private PlanParticipant requireOwnParticipant() {
		AuthenticatedUser user = currentUser.requireRole(Role.PARTICIPANT);
		return participants.findByUserId(user.id()).orElseThrow(() -> ApiException.notFound("Participant not found."));
	}

	private Account requireOwnedAccount(UUID accountId) {
		PlanParticipant participant = requireOwnParticipant();
		Account account = accounts.findById(accountId).orElseThrow(() -> ApiException.notFound("Account not found."));
		if (!account.getParticipantId().equals(participant.getId())) {
			throw ApiException.forbidden();
		}
		return account;
	}

	private PlanParticipant ownedParticipant(UUID participantId) {
		return participants.findById(participantId).orElseThrow(() -> ApiException.notFound("Participant not found."));
	}

	private SavingsPlan requireSponsoredPlan(UUID planId) {
		AuthenticatedUser user = currentUser.requireRole(Role.SPONSOR);
		SavingsPlan plan = plans.findById(planId).orElseThrow(() -> ApiException.notFound("Plan not found."));
		if (!plan.getSponsorUserId().equals(user.id())) {
			throw ApiException.forbidden();
		}
		return plan;
	}

	private AccountSummary toSummary(Account account) {
		boolean beneficiaryComplete = account.getType() != AccountType.RETIREMENT_401K
				|| beneficiaries.findByAccountId(account.getId()).stream().anyMatch(Beneficiary::isPrimaryFlag);
		return new AccountSummary(account.getId(), account.getType(), account.getName(), account.getBalance(),
				account.getDeferralPercent(), account.getYtdEmployee(), account.getYtdMatch(), beneficiaryComplete);
	}

	private BeneficiaryResponse toBeneficiary(Beneficiary beneficiary) {
		return new BeneficiaryResponse(beneficiary.getId(), beneficiary.getFullName(), beneficiary.getRelationship(),
				beneficiary.getPercent(), beneficiary.isPrimaryFlag());
	}

	private AlertResponse toAlert(ComplianceAlert alert) {
		return new AlertResponse(alert.getId(), alert.getPlanId(), alert.getParticipantId(), alert.getCode(),
				alert.getMessage(), alert.getSeverity(), alert.getStatus());
	}

	private ContributionRow toContribution(Contribution row, String participantName) {
		String accountName = accounts.findById(row.getAccountId()).map(Account::getName).orElse("Account");
		return new ContributionRow(row.getId(), row.getAccountId(), row.getParticipantId(), row.getPlanId(),
				participantName, row.getKind(), row.getAmount(), row.getPostedAt(), row.getMemo(), accountName);
	}

	private AuditRow toAudit(AuditEvent event) {
		return new AuditRow(event.getId(), event.getOccurredAt(), event.getActorUsername(), event.getActorRole(),
				event.getPlanId(), event.getParticipantId(), event.getAction(), event.getSummary());
	}

	private LoanRow toLoan(PlanLoan loan, String participantName, String accountName) {
		return new LoanRow(loan.getId(), loan.getAccountId(), loan.getParticipantId(), loan.getPlanId(),
				participantName, accountName, loan.getAmount(), loan.getStatus(), loan.getRequestedAt(),
				loan.getDecidedAt());
	}

	private void syncRetirementBeneficiaryAlert(Account account, PlanParticipant participant) {
		if (account.getType() != AccountType.RETIREMENT_401K) {
			return;
		}
		boolean hasPrimary = beneficiaries.findByAccountId(account.getId()).stream()
				.anyMatch(Beneficiary::isPrimaryFlag);
		ComplianceAlert existing = alerts
				.findByPlanIdAndParticipantIdAndCode(participant.getPlanId(), participant.getId(),
						BeneficiaryCompliance.MISSING_PRIMARY)
				.orElse(null);
		AlertStatus current = existing == null ? null : existing.getStatus();
		String missingMessage = participant.getFullName() + " has no primary beneficiary on the 401(k).";
		switch (BeneficiaryCompliance.decide(hasPrimary, current)) {
			case OPEN -> {
				alerts.save(new ComplianceAlert(UUID.randomUUID(), participant.getPlanId(), participant.getId(),
						BeneficiaryCompliance.MISSING_PRIMARY, missingMessage, "HIGH", AlertStatus.OPEN));
				audit.record(AuditAction.ALERT_OPENED, participant.getPlanId(), participant.getId(), missingMessage);
			}
			case REOPEN -> {
				existing.setStatus(AlertStatus.OPEN);
				existing.setMessage(missingMessage);
				alerts.save(existing);
				audit.record(AuditAction.ALERT_OPENED, participant.getPlanId(), participant.getId(), missingMessage);
			}
			case CLEAR -> {
				existing.setStatus(AlertStatus.CLEARED);
				alerts.save(existing);
				audit.record(AuditAction.ALERT_CLEARED, participant.getPlanId(), participant.getId(),
						"Primary beneficiary restored for " + participant.getFullName());
			}
			case LEAVE -> {
			}
		}
	}

	private String formatPercent(BigDecimal percent) {
		if (percent == null) {
			return "0";
		}
		return percent.stripTrailingZeros().toPlainString();
	}
}
