package com.silvanaya.service;

import com.silvanaya.repository.ModuleLockRepository;
import com.silvanaya.util.ApiException;
import com.silvanaya.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Service @RequiredArgsConstructor
public class ModuleLockService {
    public static final Set<String> MODULES = Set.of("customers", "orders", "shipments", "invoices", "payments");
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final SecureRandom RNG = new SecureRandom();

    private record Grant(Long userId, String module, Instant expires) {}

    private final ModuleLockRepository repo;
    private final Map<String, Grant> grants = new ConcurrentHashMap<>();

    public boolean enabled(String module) {
        return repo.findById(module).map(l -> l.getPasswordHash() != null && !l.getPasswordHash().isEmpty()).orElse(false);
    }

    public String issue(String module) {
        Instant now = Instant.now();
        grants.values().removeIf(g -> g.expires().isBefore(now));
        byte[] b = new byte[32];
        RNG.nextBytes(b);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        grants.put(token, new Grant(CurrentUser.require().getId(), module, now.plus(TTL)));
        return token;
    }


    public void require(String module, String header) {
        if (!enabled(module)) return;
        if (header != null) {
            for (String part : header.split(",")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals(module)) {
                    Grant g = grants.get(kv[1]);
                    if (g != null && g.module().equals(module)
                            && Objects.equals(g.userId(), CurrentUser.require().getId())
                            && g.expires().isAfter(Instant.now())) return;
                }
            }
        }
        throw ApiException.forbidden("This section is locked. Enter the password to continue.");
    }
}