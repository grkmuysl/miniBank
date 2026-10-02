package com.gorkemuysal.minibank.account;

import org.springframework.security.core.Authentication;

public record CurrentUser(String id, boolean operator) {
    public static CurrentUser from(Authentication auth) {
        boolean isOperator = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OPERATOR"));
        return new CurrentUser(auth.getName(), isOperator);
    }
}