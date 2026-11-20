package com.silvanaya.service;

import com.silvanaya.model.Enums.Role;
import com.silvanaya.model.Expense;
import com.silvanaya.model.User;
import com.silvanaya.repository.ExpenseRepository;
import com.silvanaya.util.ApiException;
import com.silvanaya.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;


@Service @RequiredArgsConstructor @Transactional
public class ExpenseService {
    private final ExpenseRepository repo;
    private final AuditService audit;
    private final SettingsService settings;

    private User staff() {
        User me = CurrentUser.require();
        if (me.getRole() != Role.ADMIN && me.getRole() != Role.MANAGER)
            throw ApiException.bad("Expenses can only be recorded by the Manager or System Administrator.");
        return me;
    }

    @Transactional(readOnly = true)
    public List<Expense> list(LocalDate from, LocalDate to) {
        staff();
        if (from == null || to == null || to.isBefore(from)) throw ApiException.bad("Invalid period.");
        return repo.findByDateBetweenOrderByDateDescIdDesc(from, to);
    }

    public Expense create(LocalDate date, String category, String description, BigDecimal amount,
                          String paidTo, String reference) {
        User me = staff();
        String cat = clean(category);
        if (cat.isEmpty()) throw ApiException.bad("Select an Expense Category.");
        if (cat.length() > 60) throw ApiException.bad("Expense Category is too long.");
        if (amount == null || amount.signum() <= 0) throw ApiException.bad("Amount must be greater than zero.");
        if (clean(description).length() > 500) throw ApiException.bad("Description is too long.");
        if (clean(paidTo).length() > 100 || clean(reference).length() > 100)
            throw ApiException.bad("Payee Name or Reference is too long.");

        Expense e = new Expense();
        e.setDate(date != null ? date : LocalDate.now(settings.zone()));
        e.setCategory(cat);
        e.setDescription(clean(description));
        e.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        e.setPaidTo(clean(paidTo));
        e.setReference(clean(reference));
        e.setCreatedBy(me);
        repo.save(e);
        e.setExpenseNo(settings.number("EXP", e.getId()));
        audit.log("CREATE", "Expense", e.getExpenseNo() + " - " + cat + " - TZS " + e.getAmount());
        return e;
    }

    public Expense voidExpense(Long id, String reason) {
        staff();
        Expense e = repo.findById(id).orElseThrow(() -> ApiException.notFound("Expenses"));
        if (e.isVoided()) throw ApiException.bad("This expense has already been cancelled.");
        String r = clean(reason);
        if (r.length() < 3) throw ApiException.bad("Please provide a reason for cancellation.");
        e.setVoided(true);
        e.setVoidReason(r);
        audit.log("VOID", "Expense", e.getExpenseNo() + " - " + r);
        return e;
    }

    private static String clean(String v) { return v == null ? "" : v.trim(); }
}