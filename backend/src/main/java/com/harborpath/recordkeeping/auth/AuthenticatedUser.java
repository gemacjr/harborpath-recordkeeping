package com.harborpath.recordkeeping.auth;

import java.util.UUID;

import com.harborpath.recordkeeping.domain.Role;

public record AuthenticatedUser(UUID id, String username, Role role) {
}
