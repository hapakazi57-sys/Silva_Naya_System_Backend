package com.silvanaya.controller;

import com.silvanaya.model.ModuleLock;
import com.silvanaya.repository.ModuleLockRepository;
import com.silvanaya.service.AuditService;
import com.silvanaya.util.ApiException;
import com.silvanaya.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/** Customers menu password. Any signed-in user can check status and verify; only ADMIN can set or remove it. */
@RestController @RequestMapping("/api/settings/customer-lock") @RequiredArgsConstructor
public class CustomerLockController {
    private static final String KEY = "customers";

    private final ModuleLockRepository repo;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public record LockReq(String password) {}

    private ModuleLock current() { return repo.findById(KEY).orElse(null); }
    private static boolean enabled(ModuleLock l) { return l != null && l.getPasswordHash() != null && !l.getPasswordHash().isEmpty(); }

    @GetMapping("/status")
    public Map<String, Boolean> status() { return Map.of("enabled", enabled(current())); }

    @PostMapping("/verify")
    public Map<String, Boolean> verify(@RequestBody LockReq r) {
        ModuleLock l = current();
        if (enabled(l) && (r.password() == null || !encoder.matches(r.password(), l.getPasswordHash()))) {
            audit.log("LOCK_FAILED", "Customers", "Wrong Customers menu password");
            throw ApiException.forbidden("Incorrect password");
        }
        return Map.of("ok", true);
    }

    /** ADMIN only. A blank password removes the lock. */
    @PutMapping @PreAuthorize("hasRole('ADMIN')") @Transactional
    public Map<String, Boolean> set(@RequestBody LockReq r) {
        ModuleLock l = current();
        if (l == null) { l = new ModuleLock(); l.setModule(KEY); }
        if (r.password() == null || r.password().isBlank()) {
            l.setPasswordHash(null);
            audit.log("UPDATE", "Settings", "Customers menu password removed");
        } else {
            if (r.password().length() < 4) throw ApiException.bad("Password must be at least 4 characters");
            l.setPasswordHash(encoder.encode(r.password()));
            audit.log("UPDATE", "Settings", "Customers menu password changed");
        }
        l.setUpdatedBy(CurrentUser.require().getFullName());
        l.setUpdatedAt(LocalDateTime.now());
        repo.save(l);
        return Map.of("enabled", enabled(l));
    }
}