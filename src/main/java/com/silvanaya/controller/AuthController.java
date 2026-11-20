package com.silvanaya.controller;

import com.silvanaya.dto.Dtos.*;
import com.silvanaya.model.User;
import com.silvanaya.repository.UserRepository;
import com.silvanaya.security.JwtService;
import com.silvanaya.service.AuditService;
import com.silvanaya.util.ApiException;
import com.silvanaya.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;

    @PostMapping("/login") @Transactional
    public LoginRes login(@Valid @RequestBody LoginReq r) {
        User u = users.findByEmailIgnoreCase(r.email().trim()).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.getPassword())) {
            audit.save(u == null ? null : u.getId(), r.email(), "LOGIN_FAILED", "Auth", "Login request failed");
            throw ApiException.unauthorized("Incorrect email or password");
        }
        if (!u.isActive()) throw ApiException.forbidden("Your account has been deactivated. Please contact the System Administrator.");
        u.setLastLogin(LocalDateTime.now());
        audit.save(u.getId(), u.getFullName(), "LOGIN", "Auth", "Already Login to the system");
        return new LoginRes(jwt.create(u.getEmail(), u.getRole().name()), u);
    }

    @GetMapping("/me")
    public User me() { return CurrentUser.require(); }

    @PutMapping("/profile") @Transactional
    public User profile(@Valid @RequestBody ProfileReq r) {
        User u = users.findById(CurrentUser.require().getId()).orElseThrow();
        u.setFullName(r.fullName()); u.setPhone(r.phone());
        audit.log("UPDATE", "Profile", "Changes his information");
        return u;
    }

    @PostMapping("/change-password") @Transactional
    public Map<String, String> changePassword(@Valid @RequestBody PasswordReq r) {
        User u = users.findById(CurrentUser.require().getId()).orElseThrow();
        if (!encoder.matches(r.oldPassword(), u.getPassword())) throw ApiException.bad("Incorrect password");
        u.setPassword(encoder.encode(r.newPassword()));
        audit.log("PASSWORD", "Profile", "Changes password");
        return Map.of("message", "Password changed");
    }
}
