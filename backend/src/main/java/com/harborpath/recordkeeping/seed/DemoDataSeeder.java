package com.harborpath.recordkeeping.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.harborpath.recordkeeping.SeedIds;
import com.harborpath.recordkeeping.domain.Account;
import com.harborpath.recordkeeping.domain.AccountRepository;
import com.harborpath.recordkeeping.domain.AccountType;
import com.harborpath.recordkeeping.domain.AlertStatus;
import com.harborpath.recordkeeping.domain.AppUser;
import com.harborpath.recordkeeping.domain.AppUserRepository;
import com.harborpath.recordkeeping.domain.Beneficiary;
import com.harborpath.recordkeeping.domain.BeneficiaryRepository;
import com.harborpath.recordkeeping.domain.ComplianceAlert;
import com.harborpath.recordkeeping.domain.ComplianceAlertRepository;
import com.harborpath.recordkeeping.domain.Contribution;
import com.harborpath.recordkeeping.domain.ContributionKind;
import com.harborpath.recordkeeping.domain.ContributionRepository;
import com.harborpath.recordkeeping.domain.PlanParticipant;
import com.harborpath.recordkeeping.domain.PlanParticipantRepository;
import com.harborpath.recordkeeping.domain.Role;
import com.harborpath.recordkeeping.domain.SavingsPlan;
import com.harborpath.recordkeeping.domain.SavingsPlanRepository;

@Component
public class DemoDataSeeder implements CommandLineRunner {
	private final AppUserRepository users;
	private final SavingsPlanRepository plans;
	private final PlanParticipantRepository participants;
	private final AccountRepository accounts;
	private final BeneficiaryRepository beneficiaries;
	private final ComplianceAlertRepository alerts;
	private final ContributionRepository contributions;
	private final PasswordEncoder passwordEncoder;

	public DemoDataSeeder(AppUserRepository users, SavingsPlanRepository plans,
			PlanParticipantRepository participants, AccountRepository accounts, BeneficiaryRepository beneficiaries,
			ComplianceAlertRepository alerts, ContributionRepository contributions, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.plans = plans;
		this.participants = participants;
		this.accounts = accounts;
		this.beneficiaries = beneficiaries;
		this.alerts = alerts;
		this.contributions = contributions;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		if (!users.existsById(SeedIds.MAYA_USER)) {
			seedCore();
		}
		seedLedgerIfMissing();
	}

	private void seedCore() {
		String hash = passwordEncoder.encode("harborpath");
		users.save(new AppUser(SeedIds.MAYA_USER, "maya.chen", hash, "Maya Chen", Role.PARTICIPANT));
		users.save(new AppUser(SeedIds.JORDAN_USER, "jordan.ellis", hash, "Jordan Ellis", Role.SPONSOR));
		plans.save(new SavingsPlan(SeedIds.PLAN, "Harborpath Staff 401(k)", 2026, new BigDecimal("50.00"),
				SeedIds.JORDAN_USER));
		participants.save(new PlanParticipant(SeedIds.MAYA_PARTICIPANT, SeedIds.MAYA_USER, SeedIds.PLAN, "Maya Chen",
				LocalDate.of(2018, 3, 12)));
		participants.save(new PlanParticipant(SeedIds.PRIYA_PARTICIPANT, null, SeedIds.PLAN, "Priya Shah",
				LocalDate.of(2024, 6, 3)));
		accounts.save(new Account(SeedIds.MAYA_401K, SeedIds.MAYA_PARTICIPANT, AccountType.RETIREMENT_401K,
				"Harborpath Staff 401(k)", new BigDecimal("184220.16"), new BigDecimal("8400.00"),
				new BigDecimal("4200.00"), new BigDecimal("7.00")));
		accounts.save(new Account(SeedIds.MAYA_529, SeedIds.MAYA_PARTICIPANT, AccountType.EDUCATION_529,
				"Avery Chen 529", new BigDecimal("22150.00"), BigDecimal.ZERO, BigDecimal.ZERO, null));
		accounts.save(new Account(SeedIds.PRIYA_401K, SeedIds.PRIYA_PARTICIPANT, AccountType.RETIREMENT_401K,
				"Harborpath Staff 401(k)", new BigDecimal("18440.00"), new BigDecimal("2100.00"),
				new BigDecimal("1050.00"), new BigDecimal("4.00")));
		beneficiaries.save(new Beneficiary(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), SeedIds.MAYA_401K,
				"Alex Chen", "Spouse", new BigDecimal("100.00"), true));
		alerts.save(new ComplianceAlert(SeedIds.PRIYA_ALERT, SeedIds.PLAN, SeedIds.PRIYA_PARTICIPANT,
				"MISSING_PRIMARY_BENEFICIARY", "Priya Shah has no primary beneficiary on the 401(k).", "HIGH",
				AlertStatus.OPEN));
	}

	private void seedLedgerIfMissing() {
		if (contributions.existsById(SeedIds.MAYA_YTD_EE)) {
			return;
		}
		Instant posted = Instant.parse("2026-07-31T16:00:00Z");
		contributions.save(new Contribution(SeedIds.MAYA_YTD_EE, SeedIds.MAYA_401K, SeedIds.MAYA_PARTICIPANT,
				SeedIds.PLAN, ContributionKind.PAYROLL_EMPLOYEE, new BigDecimal("8400.00"), posted,
				"YTD employee deferrals through July"));
		contributions.save(new Contribution(SeedIds.MAYA_YTD_MATCH, SeedIds.MAYA_401K, SeedIds.MAYA_PARTICIPANT,
				SeedIds.PLAN, ContributionKind.PAYROLL_MATCH, new BigDecimal("4200.00"), posted,
				"YTD employer match through July"));
		contributions.save(new Contribution(SeedIds.PRIYA_YTD_EE, SeedIds.PRIYA_401K, SeedIds.PRIYA_PARTICIPANT,
				SeedIds.PLAN, ContributionKind.PAYROLL_EMPLOYEE, new BigDecimal("2100.00"), posted,
				"YTD employee deferrals through July"));
		contributions.save(new Contribution(SeedIds.PRIYA_YTD_MATCH, SeedIds.PRIYA_401K, SeedIds.PRIYA_PARTICIPANT,
				SeedIds.PLAN, ContributionKind.PAYROLL_MATCH, new BigDecimal("1050.00"), posted,
				"YTD employer match through July"));
	}
}
