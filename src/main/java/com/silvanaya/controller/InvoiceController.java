package com.silvanaya.controller;

import com.silvanaya.dto.Dtos.*;
import com.silvanaya.model.Invoice;
import com.silvanaya.repository.InvoiceRepository;
import com.silvanaya.service.InvoiceService;
import com.silvanaya.service.ModuleLockService;
import com.silvanaya.service.OptionsMapper;
import com.silvanaya.util.ApiException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/invoices") @RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceRepository invoices;
    private final InvoiceService service;
    private final ModuleLockService locks;
    private final OptionsMapper opts;

    @GetMapping
    public List<Invoice> list(@RequestHeader(value = "X-Module-Tokens", required = false) String tk) {
        locks.require("invoices", tk);
        return invoices.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    /** Slim list for dropdowns in other forms (no lock). */
    @GetMapping("/options")
    public List<Map<String, Object>> options() {
        return invoices.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(i -> opts.pick(i, "id", "invoiceNo", "voided", "balance", "purchaseBalance", "shippingBalance")).toList();
    }

    @GetMapping("/{id}")
    public Invoice get(@PathVariable Long id,
                       @RequestHeader(value = "X-Module-Tokens", required = false) String tk) {
        locks.require("invoices", tk);
        return invoices.findById(id).orElseThrow(() -> ApiException.notFound("Ankara"));
    }

    @PostMapping @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    public Invoice create(@Valid @RequestBody InvoiceReq r) { return service.create(r); }

    @PostMapping("/{id}/void") @PreAuthorize("hasRole('MANAGER')")
    public Invoice voidInvoice(@PathVariable Long id, @Valid @RequestBody ReasonReq r) {
        return service.voidInvoice(id, r.reason());
    }
}