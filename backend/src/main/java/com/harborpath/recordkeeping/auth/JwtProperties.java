package com.harborpath.recordkeeping.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "harborpath.jwt")
public class JwtProperties {
	private String secret;
	private long ttlSeconds = 28800;

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public long getTtlSeconds() {
		return ttlSeconds;
	}

	public void setTtlSeconds(long ttlSeconds) {
		this.ttlSeconds = ttlSeconds;
	}
}
