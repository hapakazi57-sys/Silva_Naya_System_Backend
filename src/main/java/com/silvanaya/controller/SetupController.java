package com.silvanaya.controller;

import com.silvanaya.dto.Dtos.UserReq;
import com.silvanaya.model.Enums.Role;
import com.silvanaya.model.User;
import com.silvanaya.repository.UserRepository;
import com.silvanaya.util.ApiException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class SetupController {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @PostMapping("/setup") @Transactional
    public User setup(@Valid @RequestBody UserReq r) {
        if (users.count() > 0) throw ApiException.forbidden("System Already prepared");
        if (r.password() == null || r.password().length() < 8)
            throw ApiException.bad("Password should be at-least 8 characters");
        User u = new User();
        u.setFullName(r.fullName());
        u.setEmail(r.email().trim().toLowerCase());
        u.setPhone(r.phone());
        u.setRole(Role.ADMIN);              // daima ADMIN, bila kujali role iliyotumwa
        u.setPassword(encoder.encode(r.password()));
        return users.save(u);
    }
}