package com.silvanaya.controller;

import com.silvanaya.dto.Dtos.*;
import com.silvanaya.model.Payment;
import com.silvanaya.repository.PaymentRepository;
import com.silvanaya.service.ModuleLockService;
import com.silvanaya.service.PaymentService;
import com.silvanaya.util.ApiException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/payments") @RequiredArgsConstructor
public class PaymentController {
    private final PaymentRepository payments;
    private final PaymentService service;
    private final ModuleLockService locks;

    @GetMapping
    public List<Payment> list(@RequestHeader(value = "X-Module-Tokens", required = false) String tk) {
        locks.require("payments", tk);
        return payments.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @GetMapping("/{id}")
    public Payment get(@PathVariable Long id,
                       @RequestHeader(value = "X-Module-Tokens", required = false) String tk) {
        locks.require("payments", tk);
        return payments.findById(id).orElseThrow(() -> ApiException.notFound("Payment"));
    }

    @PostMapping @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    public Payment create(@Valid @RequestBody PaymentReq r) { return service.create(r); }

    @PostMapping("/{id}/void") @PreAuthorize("hasRole('MANAGER')")
    public Payment voidReceipt(@PathVariable Long id, @Valid @RequestBody ReasonReq r) {
        return service.voidReceipt(id, r.reason());
    }
}