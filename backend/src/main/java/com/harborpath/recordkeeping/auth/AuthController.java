package com.harborpath.recordkeeping.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harborpath.recordkeeping.domain.AppUser;
import com.harborpath.recordkeeping.domain.AppUserRepository;
import com.harborpath.recordkeeping.web.ApiException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
	private final AppUserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final CurrentUser currentUser;

	public AuthController(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
			CurrentUser currentUser) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.currentUser = currentUser;
	}

	@PostMapping("/auth/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		AppUser user = users.findByUsername(request.username()).orElseThrow(ApiException::unauthorized);
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw ApiException.unauthorized();
		}
		return new LoginResponse(jwtService.issue(user), "Bearer", jwtService.ttlSeconds(), user.getRole(),
				user.getDisplayName());
	}

	@GetMapping("/me")
	public MeResponse me() {
		AuthenticatedUser auth = currentUser.require();
		AppUser user = users.findById(auth.id()).orElseThrow(ApiException::unauthorized);
		return new MeResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
	}
}
