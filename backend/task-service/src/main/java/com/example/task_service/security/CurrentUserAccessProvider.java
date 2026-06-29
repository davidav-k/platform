package com.example.task_service.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class CurrentUserAccessProvider {

    private static final Set<String> ADMIN_ROLES = Set.of("ROLE_ADMIN", "ROLE_SUPER_ADMIN");

    public CurrentUserAccess currentUserAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required");
        }

        boolean admin = authentication.getAuthorities().stream()
            .anyMatch(authority -> ADMIN_ROLES.contains(authority.getAuthority()));
        return new CurrentUserAccess(authenticatedUser.userId(), admin);
    }

    public record CurrentUserAccess(
            UUID userId,
            boolean admin) {

        public boolean canAccess(UUID createdByUserId, UUID assigneeUserId) {
            return admin || userId.equals(createdByUserId) || userId.equals(assigneeUserId);
        }

        public boolean canManage(UUID createdByUserId) {
            return admin || userId.equals(createdByUserId);
        }
    }
}
