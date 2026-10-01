package com.harborpath.recordkeeping.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.harborpath.recordkeeping.domain.Role;
import com.harborpath.recordkeeping.web.ApiException;

@Component
public class CurrentUser {
	public AuthenticatedUser require() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
			throw ApiException.unauthorized();
		}
		return user;
	}

	public AuthenticatedUser requireRole(Role role) {
		AuthenticatedUser user = require();
		if (user.role() != role) {
			throw ApiException.forbidden();
		}
		return user;
	}
}
