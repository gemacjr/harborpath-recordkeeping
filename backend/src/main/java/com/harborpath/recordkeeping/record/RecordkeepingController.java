package com.harborpath.recordkeeping.record;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RecordkeepingController {
	private final RecordkeepingService service;

	public RecordkeepingController(RecordkeepingService service) {
		this.service = service;
	}

	@GetMapping("/households/me")
	public HouseholdResponse household() {
		return service.household();
	}

	@GetMapping("/accounts/{id}")
	public AccountSummary account(@PathVariable UUID id) {
		return service.account(id);
	}

	@PatchMapping("/accounts/{id}/deferral")
	public AccountSummary deferral(@PathVariable UUID id, @RequestBody DeferralWrite body) {
		return service.updateDeferral(id, body.percent());
	}

	@GetMapping("/accounts/{id}/beneficiaries")
	public List<BeneficiaryResponse> beneficiaries(@PathVariable UUID id) {
		return service.beneficiaries(id);
	}

	@PutMapping("/accounts/{id}/beneficiaries")
	public List<BeneficiaryResponse> replaceBeneficiaries(@PathVariable UUID id,
			@RequestBody List<BeneficiaryWrite> body) {
		return service.replaceBeneficiaries(id, body);
	}

	@GetMapping("/plans")
	public List<PlanSummary> plans() {
		return service.plans();
	}

	@GetMapping("/plans/{id}/census")
	public List<CensusRow> census(@PathVariable UUID id) {
		return service.census(id);
	}

	@GetMapping("/plans/{id}/alerts")
	public List<AlertResponse> alerts(@PathVariable UUID id) {
		return service.alerts(id);
	}

	@PatchMapping("/alerts/{id}")
	public AlertResponse acknowledge(@PathVariable UUID id, @RequestBody AlertStatusWrite body) {
		return service.acknowledge(id);
	}

	@GetMapping("/contributions/me")
	public List<ContributionRow> myContributions() {
		return service.myContributions();
	}

	@GetMapping("/plans/{id}/contributions")
	public List<ContributionRow> planContributions(@PathVariable UUID id) {
		return service.planContributions(id);
	}

	@PostMapping("/plans/{id}/payroll")
	public PayrollResult payroll(@PathVariable UUID id, @RequestBody PayrollWrite body) {
		return service.postPayroll(id, body.periodPay());
	}

	@PostMapping("/accounts/{id}/contributions")
	public ContributionRow educationContribution(@PathVariable UUID id, @RequestBody AmountWrite body) {
		return service.contributeEducation(id, body.amount());
	}

	@PostMapping("/accounts/{id}/withdrawals")
	public ContributionRow educationWithdrawal(@PathVariable UUID id, @RequestBody WithdrawalWrite body) {
		return service.withdrawEducation(id, body.amount(), body.memo());
	}

	@GetMapping("/audit/me")
	public List<AuditRow> myAudit() {
		return service.myAudit();
	}

	@GetMapping("/plans/{id}/audit")
	public List<AuditRow> planAudit(@PathVariable UUID id) {
		return service.planAudit(id);
	}

	@PostMapping("/accounts/{id}/loans")
	public LoanRow requestLoan(@PathVariable UUID id, @RequestBody AmountWrite body) {
		return service.requestLoan(id, body.amount());
	}

	@GetMapping("/loans/me")
	public List<LoanRow> myLoans() {
		return service.myLoans();
	}

	@GetMapping("/plans/{id}/loans")
	public List<LoanRow> planLoans(@PathVariable UUID id) {
		return service.planLoans(id);
	}

	@PatchMapping("/loans/{id}")
	public LoanRow decideLoan(@PathVariable UUID id, @RequestBody LoanDecisionWrite body) {
		return service.decideLoan(id, body.decision());
	}
}
