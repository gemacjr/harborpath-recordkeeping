package com.harborpath.recordkeeping.auth;

import com.harborpath.recordkeeping.domain.Role;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, Role role, String displayName) {
}
