package com.silvanaya.controller;

import com.silvanaya.model.ModuleLock;
import com.silvanaya.repository.ModuleLockRepository;
import com.silvanaya.service.AuditService;
import com.silvanaya.service.ModuleLockService;
import com.silvanaya.util.ApiException;
import com.silvanaya.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/** Any signed-in user can check status and verify. Only ADMIN can set or remove a password. */
@RestController @RequestMapping("/api/settings/module-lock") @RequiredArgsConstructor
public class ModuleLockController {
    private final ModuleLockRepository repo;
    private final ModuleLockService locks;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public record LockReq(String password) {}

    private static String valid(String m) {
        if (!ModuleLockService.MODULES.contains(m)) throw ApiException.bad("Unknown module");
        return m;
    }

    @GetMapping("/{module}/status")
    public Map<String, Object> status(@PathVariable String module) {
        return Map.of("enabled", locks.enabled(valid(module)));
    }

    @PostMapping("/{module}/verify")
    public Map<String, Object> verify(@PathVariable String module, @RequestBody LockReq r) {
        valid(module);
        ModuleLock l = repo.findById(module).orElse(null);
        if (locks.enabled(module) && (r.password() == null || !encoder.matches(r.password(), l.getPasswordHash()))) {
            audit.log("LOCK_FAILED", module, "Wrong " + module + " menu password");
            throw ApiException.forbidden("Incorrect password");
        }
        return Map.of("ok", true, "token", locks.issue(module));
    }

    /** ADMIN only. A blank password removes the lock. */
    @PutMapping("/{module}") @PreAuthorize("hasRole('ADMIN')") @Transactional
    public Map<String, Object> set(@PathVariable String module, @RequestBody LockReq r) {
        valid(module);
        ModuleLock l = repo.findById(module).orElseGet(() -> { ModuleLock n = new ModuleLock(); n.setModule(module); return n; });
        if (r.password() == null || r.password().isBlank()) {
            l.setPasswordHash(null);
            audit.log("UPDATE", "Settings", module + " menu password removed");
        } else {
            if (r.password().length() < 4) throw ApiException.bad("Password must be at least 4 characters");
            l.setPasswordHash(encoder.encode(r.password()));
            audit.log("UPDATE", "Settings", module + " menu password changed");
        }
        l.setUpdatedBy(CurrentUser.require().getFullName());
        l.setUpdatedAt(LocalDateTime.now());
        repo.save(l);
        return Map.of("enabled", locks.enabled(module));
    }
}