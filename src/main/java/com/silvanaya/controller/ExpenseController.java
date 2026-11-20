package com.silvanaya.controller;

import com.silvanaya.model.Expense;
import com.silvanaya.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController @RequestMapping("/api/expenses") @RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService service;

    public record ExpenseReq(LocalDate date, String category, String description, BigDecimal amount,
                             String paidTo, String reference) {}

    public record VoidReq(String reason) {}

    public record ExpenseView(Long id, String expenseNo, LocalDate date, String category, String description,
                              BigDecimal amount, String paidTo, String reference, boolean voided,
                              String voidReason, String createdBy, LocalDateTime createdAt) {
        static ExpenseView of(Expense e) {
            return new ExpenseView(e.getId(), e.getExpenseNo(), e.getDate(), e.getCategory(), e.getDescription(),
                    e.getAmount(), e.getPaidTo(), e.getReference(), e.isVoided(), e.getVoidReason(),
                    e.getCreatedBy() == null ? "" : e.getCreatedBy().getFullName(), e.getCreatedAt());
        }
    }

    @GetMapping
    public List<ExpenseView> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(from, to).stream().map(ExpenseView::of).toList();
    }

    @PostMapping
    public ExpenseView create(@RequestBody ExpenseReq r) {
        return ExpenseView.of(service.create(r.date(), r.category(), r.description(), r.amount(), r.paidTo(), r.reference()));
    }

    @PostMapping("/{id}/void")
    public ExpenseView voidExpense(@PathVariable Long id, @RequestBody VoidReq r) {
        return ExpenseView.of(service.voidExpense(id, r.reason()));
    }
}