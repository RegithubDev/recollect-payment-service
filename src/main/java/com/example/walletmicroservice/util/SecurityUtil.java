package com.example.walletmicroservice.util;

import com.example.walletmicroservice.config.ExternalTokenValidationFilter.UserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Optional;

public class SecurityUtil {

    public static Optional<UserInfo> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
                authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal()) &&
                authentication.getPrincipal() instanceof UserInfo) {
            return Optional.of((UserInfo) authentication.getPrincipal());
        }

        return Optional.empty();
    }

    public static String getCurrentUserId() {
        return getCurrentUser()
                .map(UserInfo::getUid)
                .orElse(null);
    }

    public static String getCurrentUserRole() {
        return getCurrentUser()
                .map(UserInfo::getRole)
                .orElse(null);
    }

    public static boolean isAdmin() {
        return getCurrentUser()
                .map(user -> "ADMIN".equalsIgnoreCase(user.getRole()))
                .orElse(false);
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roleWithPrefix = "ROLE_" + role.toUpperCase();

        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(roleWithPrefix));
    }

    public static boolean isCurrentUser(String userId) {
        String currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }
}