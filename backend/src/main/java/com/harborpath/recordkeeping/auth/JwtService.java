package com.harborpath.recordkeeping.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.harborpath.recordkeeping.domain.AppUser;
import com.harborpath.recordkeeping.domain.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	private final JwtProperties properties;
	private final SecretKey key;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
	}

	public String issue(AppUser user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getUsername())
				.claim("role", user.getRole().name())
				.claim("uid", user.getId().toString())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(properties.getTtlSeconds())))
				.signWith(key)
				.compact();
	}

	public Claims parse(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}

	public AuthenticatedUser fromToken(String token) {
		Claims claims = parse(token);
		return new AuthenticatedUser(
				UUID.fromString(claims.get("uid", String.class)),
				claims.getSubject(),
				Role.valueOf(claims.get("role", String.class)));
	}

	public long ttlSeconds() {
		return properties.getTtlSeconds();
	}
}
