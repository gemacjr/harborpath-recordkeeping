package com.harborpath.recordkeeping.auth;

import java.util.UUID;

import com.harborpath.recordkeeping.domain.Role;

public record MeResponse(UUID id, String username, String displayName, Role role) {
}
