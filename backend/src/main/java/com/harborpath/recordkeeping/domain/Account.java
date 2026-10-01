package com.harborpath.recordkeeping.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "account")
public class Account {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID participantId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AccountType type;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal balance;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal ytdEmployee;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal ytdMatch;

	@Column(precision = 5, scale = 2)
	private BigDecimal deferralPercent;

	protected Account() {
	}

	public Account(UUID id, UUID participantId, AccountType type, String name, BigDecimal balance,
			BigDecimal ytdEmployee, BigDecimal ytdMatch, BigDecimal deferralPercent) {
		this.id = id;
		this.participantId = participantId;
		this.type = type;
		this.name = name;
		this.balance = balance;
		this.ytdEmployee = ytdEmployee;
		this.ytdMatch = ytdMatch;
		this.deferralPercent = deferralPercent;
	}

	public UUID getId() {
		return id;
	}

	public UUID getParticipantId() {
		return participantId;
	}

	public AccountType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public BigDecimal getYtdEmployee() {
		return ytdEmployee;
	}

	public BigDecimal getYtdMatch() {
		return ytdMatch;
	}

	public BigDecimal getDeferralPercent() {
		return deferralPercent;
	}

	public void setDeferralPercent(BigDecimal deferralPercent) {
		this.deferralPercent = deferralPercent;
	}

	public void credit(BigDecimal amount) {
		this.balance = this.balance.add(amount);
	}

	public void debit(BigDecimal amount) {
		this.balance = this.balance.subtract(amount);
	}

	public void addYtdEmployee(BigDecimal amount) {
		this.ytdEmployee = this.ytdEmployee.add(amount);
	}

	public void addYtdMatch(BigDecimal amount) {
		this.ytdMatch = this.ytdMatch.add(amount);
	}
}
