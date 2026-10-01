package com.harborpath.recordkeeping.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class AppUser {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false)
	private String passwordHash;

	@Column(nullable = false)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	protected AppUser() {
	}

	public AppUser(UUID id, String username, String passwordHash, String displayName, Role role) {
		this.id = id;
		this.username = username;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.role = role;
	}

	public UUID getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Role getRole() {
		return role;
	}
}
