package com.silvanaya.util;

import com.silvanaya.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}
    public static User get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.getPrincipal() instanceof User u) ? u : null;
    }
    public static User require() {
        User u = get();
        if (u == null) throw ApiException.unauthorized("Please login to the system");
        return u;
    }
    /** Unique document number, e.g. PO-2026-00012 */
    public static String number(String prefix, Long id) {
        return prefix + "-" + java.time.Year.now().getValue() + "-" + String.format("%05d", id);
    }
}