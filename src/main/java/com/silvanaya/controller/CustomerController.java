package com.silvanaya.controller;

import com.silvanaya.dto.Dtos.CustomerReq;
import com.silvanaya.model.*;
import com.silvanaya.repository.*;
import com.silvanaya.service.AuditService;
import com.silvanaya.service.ModuleLockService;
import com.silvanaya.util.ApiException;
import com.silvanaya.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController @RequestMapping("/api/customers") @RequiredArgsConstructor @Transactional
public class CustomerController {
    private final CustomerRepository customers;
    private final PurchaseOrderRepository orders;
    private final ShipmentRepository shipments;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final AuditService audit;
    private final ModuleLockService locks;

    @GetMapping
    public List<Customer> list(@RequestHeader(value = "X-Module-Tokens", required = false) String tk) {
        locks.require("customers", tk);
        return customers.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    /** Slim list for dropdowns in other forms: no phone, address or email. No lock. */
    @GetMapping("/options")
    public List<Map<String, Object>> options() {
        return customers.findAll(Sort.by(Sort.Direction.ASC, "name")).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("customerNo", c.getCustomerNo());
            return m;
        }).toList();
    }

    @PostMapping
    public Customer create(@Valid @RequestBody CustomerReq r) {
        Customer c = new Customer();
        apply(c, r);
        customers.save(c);
        c.setCustomerNo(CurrentUser.number("SN-C", c.getId()));
        audit.log("CREATE", "Customer", c.getCustomerNo() + " - " + c.getName());
        return c;
    }

    @PutMapping("/{id}")
    public Customer update(@PathVariable Long id, @Valid @RequestBody CustomerReq r,
                           @RequestHeader(value = "X-Module-Tokens", required = false) String tk) {
        locks.require("customers", tk);
        Customer c = customers.findById(id).orElseThrow(() -> ApiException.notFound("Customer"));
        apply(c, r);
        audit.log("UPDATE", "Customer", c.getCustomerNo() + " - " + c.getName());
        return c;
    }

    /** Full customer history: orders, shipments, invoices, payments and debt. */
    @GetMapping("/{id}/summary")
    public Map<String, Object> summary(@PathVariable Long id,
                                       @RequestHeader(value = "X-Module-Tokens", required = false) String tk) {
        locks.require("customers", tk);
        Customer c = customers.findById(id).orElseThrow(() -> ApiException.notFound("Customer"));
        List<Invoice> inv = invoices.findByCustomerIdOrderByIdDesc(id);
        List<Payment> pay = payments.findByCustomerIdOrderByIdDesc(id);
        BigDecimal debt = inv.stream().filter(i -> !i.isVoided()).map(Invoice::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = pay.stream().filter(p -> !p.isReceiptVoided()).map(Payment::getAmountTzs).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customer", c);
        m.put("orders", orders.findByCustomerIdOrderByIdDesc(id));
        m.put("shipments", shipments.findByCustomerIdOrderByIdDesc(id));
        m.put("invoices", inv);
        m.put("payments", pay);
        m.put("debt", debt);
        m.put("totalPaid", paid);
        return m;
    }

    private void apply(Customer c, CustomerReq r) {
        c.setName(r.name().trim()); c.setPhone(r.phone().trim());
        c.setEmail(r.email()); c.setAddress(r.address());
    }
}