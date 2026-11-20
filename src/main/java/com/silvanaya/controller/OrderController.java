package com.silvanaya.controller;

import com.silvanaya.dto.Dtos.*;
import com.silvanaya.model.*;
import com.silvanaya.model.Enums.OrderStatus;
import com.silvanaya.repository.*;
import com.silvanaya.service.AuditService;
import com.silvanaya.service.ModuleLockService;
import com.silvanaya.service.OptionsMapper;
import com.silvanaya.util.ApiException;
import com.silvanaya.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor @Transactional
public class OrderController {
    private final PurchaseOrderRepository orders;
    private final CustomerRepository customers;
    private final AuditService audit;
    private final ModuleLockService locks;
    private final OptionsMapper opts;

    @GetMapping
    public List<PurchaseOrder> list(@RequestHeader(value = "X-Module-Tokens", required = false) String tk) {
        locks.require("orders", tk);
        return orders.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    /** Slim list for dropdowns in other forms (no lock). */
    @GetMapping("/options")
    public List<Map<String, Object>> options() {
        return orders.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(o -> opts.pick(o, "id", "orderNo", "supplierName", "status", "currency", "totalAmount", "actualCost")).toList();
    }

    @GetMapping("/{id}")
    public PurchaseOrder get(@PathVariable Long id,
                             @RequestHeader(value = "X-Module-Tokens", required = false) String tk) {
        locks.require("orders", tk);
        return find(id);
    }

    @PostMapping
    public PurchaseOrder create(@Valid @RequestBody OrderReq r) {
        PurchaseOrder o = new PurchaseOrder();
        o.setCreatedBy(CurrentUser.require());
        fill(o, r);
        orders.save(o);
        o.setOrderNo(CurrentUser.number("PO", o.getId()));
        audit.log("CREATE", "Order", o.getOrderNo() + " - " + o.getCustomer().getName());
        return o;
    }

    @PutMapping("/{id}")
    public PurchaseOrder update(@PathVariable Long id, @Valid @RequestBody OrderReq r) {
        PurchaseOrder o = find(id);
        if (o.getStatus() != OrderStatus.DRAFT && o.getStatus() != OrderStatus.REVISION)
            throw ApiException.bad("An order can only be modified while it is in Draft or Returned for Revision status.");
        fill(o, r);
        audit.log("UPDATE", "Order", o.getOrderNo());
        return o;
    }

    @PostMapping("/{id}/submit")
    public PurchaseOrder submit(@PathVariable Long id) {
        PurchaseOrder o = find(id);
        if (o.getStatus() != OrderStatus.DRAFT && o.getStatus() != OrderStatus.REVISION)
            throw ApiException.bad("This order cannot be submitted at this time.");
        o.setStatus(OrderStatus.SUBMITTED); o.setManagerNote(null); o.setUpdatedAt(LocalDateTime.now());
        audit.log("SUBMIT", "Order", o.getOrderNo());
        return o;
    }

    @PostMapping("/{id}/approve") @PreAuthorize("hasRole('MANAGER')")
    public PurchaseOrder approve(@PathVariable Long id) {
        PurchaseOrder o = decideCheck(id);
        o.setStatus(OrderStatus.APPROVED); o.setApprovedBy(CurrentUser.require()); o.setUpdatedAt(LocalDateTime.now());
        audit.log("APPROVE", "Order", o.getOrderNo());
        return o;
    }

    @PostMapping("/{id}/reject") @PreAuthorize("hasRole('MANAGER')")
    public PurchaseOrder reject(@PathVariable Long id, @RequestBody NoteReq r) {
        PurchaseOrder o = decideCheck(id);
        o.setStatus(OrderStatus.REJECTED); o.setManagerNote(r.note()); o.setApprovedBy(CurrentUser.require());
        o.setUpdatedAt(LocalDateTime.now());
        audit.log("REJECT", "Order", o.getOrderNo() + " - " + r.note());
        return o;
    }

    @PostMapping("/{id}/revision") @PreAuthorize("hasRole('MANAGER')")
    public PurchaseOrder revision(@PathVariable Long id, @RequestBody NoteReq r) {
        PurchaseOrder o = decideCheck(id);
        o.setStatus(OrderStatus.REVISION); o.setManagerNote(r.note()); o.setUpdatedAt(LocalDateTime.now());
        audit.log("REVISION", "Order", o.getOrderNo() + " - " + r.note());
        return o;
    }

    @PostMapping("/{id}/actual-cost") @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    public PurchaseOrder actualCost(@PathVariable Long id, @Valid @RequestBody AmountReq r) {
        PurchaseOrder o = find(id);
        if (o.getStatus() != OrderStatus.APPROVED) throw ApiException.bad("Actual costs are recorded only for approved orders.");
        o.setActualCost(r.amount());
        audit.log("ACTUAL_COST", "Order", o.getOrderNo() + " - " + r.amount());
        return o;
    }

    // ---- helpers ----
    private PurchaseOrder decideCheck(Long id) {
        PurchaseOrder o = find(id);
        if (o.getStatus() != OrderStatus.SUBMITTED) throw ApiException.bad("Oder waits for approval");
        if (o.getCreatedBy() != null && o.getCreatedBy().getId().equals(CurrentUser.require().getId()))
            throw ApiException.forbidden("You cannot approve an order that you created yourself.");
        return o;
    }

    private PurchaseOrder find(Long id) { return orders.findById(id).orElseThrow(() -> ApiException.notFound("Oder")); }

    private void fill(PurchaseOrder o, OrderReq r) {
        o.setCustomer(customers.findById(r.customerId()).orElseThrow(() -> ApiException.notFound("Customer")));
        o.setCountry(r.country().toUpperCase()); o.setSupplierName(r.supplierName());
        o.setCurrency(r.currency().toUpperCase()); o.setNote(r.note());
        o.getItems().clear();
        for (ItemReq i : r.items()) {
            OrderItem it = new OrderItem();
            it.setName(i.name()); it.setDescription(i.description());
            it.setQuantity(i.quantity()); it.setUnitPrice(i.unitPrice());
            o.getItems().add(it);
        }
        o.recalc();
        o.setUpdatedAt(LocalDateTime.now());
    }
}