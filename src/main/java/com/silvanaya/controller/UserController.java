package com.silvanaya.controller;

import com.silvanaya.dto.Dtos.*;
import com.silvanaya.model.User;
import com.silvanaya.repository.UserRepository;
import com.silvanaya.service.AuditService;
import com.silvanaya.util.ApiException;
import com.silvanaya.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/users") @RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") @Transactional
public class UserController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    @GetMapping
    public List<User> list() { return users.findAll(Sort.by(Sort.Direction.DESC, "id")); }

    @PostMapping
    public User create(@Valid @RequestBody UserReq r) {
        if (r.password() == null || r.password().length() < 8) throw ApiException.bad("Password should be at-least 8 characters");
        if (users.findByEmailIgnoreCase(r.email()).isPresent()) throw ApiException.bad("Email used");
        User u = new User();
        u.setFullName(r.fullName()); u.setEmail(r.email().trim().toLowerCase()); u.setPhone(r.phone());
        u.setRole(r.role()); u.setPassword(encoder.encode(r.password()));
        users.save(u);
        audit.log("CREATE", "User", u.getEmail() + " (" + u.getRole() + ")");
        return u;
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @Valid @RequestBody UserReq r) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        boolean self = u.getId().equals(CurrentUser.require().getId());
        if (self && (Boolean.FALSE.equals(r.active()) || r.role() != u.getRole()))
            throw ApiException.bad("You cannot deactivate your own account or change your own role.");
        u.setFullName(r.fullName()); u.setPhone(r.phone()); u.setRole(r.role());
        if (r.active() != null) u.setActive(r.active());
        audit.log("UPDATE", "User", u.getEmail() + " (" + u.getRole() + ", active=" + u.isActive() + ")");
        return u;
    }

    @PostMapping("/{id}/reset-password")
    public Map<String, String> reset(@PathVariable Long id, @Valid @RequestBody ResetReq r) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        u.setPassword(encoder.encode(r.password()));
        audit.log("PASSWORD_RESET", "User", u.getEmail());
        return Map.of("message", "Password returned");
    }
}
