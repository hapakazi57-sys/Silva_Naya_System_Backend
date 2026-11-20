package com.silvanaya.service;

import com.silvanaya.dto.ReportData;
import com.silvanaya.dto.ReportData.Col;
import com.silvanaya.dto.ReportData.Stat;
import com.silvanaya.model.*;
import com.silvanaya.model.Enums.Role;
import com.silvanaya.util.ApiException;
import com.silvanaya.util.CurrentUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


@Service @RequiredArgsConstructor @Transactional
public class ReportService {

    @PersistenceContext private EntityManager em;
    private final SettingsService settings;

    private record Ctx(String periodName, LocalDate from, LocalDate to, LocalDateTime start, LocalDateTime end,
                       User me, LocalDate today) {}

    public ReportData build(String type, String period, LocalDate date, LocalDate from, LocalDate to) {
        User me = CurrentUser.require();
        if (me.getRole() != Role.ADMIN && me.getRole() != Role.MANAGER)
            throw ApiException.bad("Reports are accessible only to Managers and System Administrators.");

        LocalDate today = LocalDate.now(settings.zone());
        LocalDate base = date != null ? date : today;
        String p = period == null ? "monthly" : period.toLowerCase(Locale.ROOT);

        LocalDate f;
        LocalDate t;
        switch (p) {
            case "weekly" -> { f = base.with(DayOfWeek.MONDAY); t = f.plusDays(6); }
            case "monthly" -> { f = base.withDayOfMonth(1); t = base.with(TemporalAdjusters.lastDayOfMonth()); }
            case "yearly" -> { f = base.withDayOfYear(1); t = base.with(TemporalAdjusters.lastDayOfYear()); }
            case "custom" -> {
                if (from == null || to == null) throw ApiException.bad("Please specify the start and end dates.");
                if (to.isBefore(from)) throw ApiException.bad("The end date cannot be earlier than the start date.");
                if (from.plusYears(5).isBefore(to)) throw ApiException.bad("The selected date range exceeds the maximum limit of five years.");
                f = from;
                t = to;
            }
            default -> throw ApiException.bad("Invalid period type.");
        }

        Ctx c = new Ctx(p, f, t, f.atStartOfDay(), t.plusDays(1).atStartOfDay(), me, today);
        return switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
            case "overview" -> overview(c);
            case "revenue" -> revenue(c);
            case "expenses" -> expenses(c);
            case "debts" -> debts(c);
            case "customers" -> customers(c);
            case "employees" -> employees(c);
            case "shipments" -> shipments(c);
            default -> throw ApiException.bad("Report type is required.");
        };
    }

    /* ======================= OVERVIEW ======================= */
    private ReportData overview(Ctx c) {
        List<Payment> pays = payments(c);
        List<Expense> exps = expenseList(c);
        boolean monthly = ChronoUnit.DAYS.between(c.from(), c.to()) > 62;

        Map<String, BigDecimal[]> buckets = new LinkedHashMap<>();
        for (LocalDate d = c.from(); !d.isAfter(c.to()); d = monthly ? d.plusMonths(1).withDayOfMonth(1) : d.plusDays(1))
            buckets.put(key(d, monthly), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});

        BigDecimal rev = BigDecimal.ZERO;
        BigDecimal exp = BigDecimal.ZERO;
        for (Payment p : pays) {
            BigDecimal[] b = buckets.computeIfAbsent(key(p.getPaidAt().toLocalDate(), monthly),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            b[0] = b[0].add(nz(p.getAmountTzs()));
            rev = rev.add(nz(p.getAmountTzs()));
        }
        for (Expense e : exps) {
            BigDecimal[] b = buckets.computeIfAbsent(key(e.getDate(), monthly),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            b[1] = b[1].add(nz(e.getAmount()));
            exp = exp.add(nz(e.getAmount()));
        }

        List<List<Object>> rows = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> en : buckets.entrySet()) {
            BigDecimal[] b = en.getValue();
            rows.add(row(en.getKey(), b[0], b[1], b[0].subtract(b[1])));
        }

        BigDecimal invoiced = scalar("select coalesce(sum(i.total),0) from Invoice i where i.voided = false "
                + "and i.issueDate >= :f and i.issueDate <= :t", "f", c.from(), "t", c.to());
        BigDecimal debt = scalar("select coalesce(sum(i.total - i.purchasePaid - i.shippingPaid),0) "
                + "from Invoice i where i.voided = false");
        long ships = count("select count(s) from Shipment s where s.createdAt >= :st and s.createdAt < :en",
                "st", c.start(), "en", c.end());
        long newCust = count("select count(x) from Customer x where x.createdAt >= :st and x.createdAt < :en",
                "st", c.start(), "en", c.end());

        List<Col> cols = List.of(
                col("period", monthly ? "Month" : "Date", "text", "left"),
                col("revenue", "Revenue (TZS)", "money", "right"),
                col("expenses", "Expenses (TZS)", "money", "right"),
                col("net", "Net (TZS)", "money", "right"));
        List<Stat> stats = List.of(
                new Stat("Revenue received", money(rev)),
                new Stat("Expenses", money(exp)),
                new Stat("Net result", money(rev.subtract(exp))),
                new Stat("Invoiced in period", money(invoiced)),
                new Stat("Outstanding debt (now)", money(debt)),
                new Stat("Shipments registered", String.valueOf(ships)),
                new Stat("New customers", String.valueOf(newCust)));
        return make(c, "overview", "Overview Report", cols, rows, stats);
    }

    /* ======================= REVENUE ======================= */
    private ReportData revenue(Ctx c) {
        List<Payment> list = payments(c);
        List<Col> cols = List.of(
                col("date", "Date", "date", "left"),
                col("receiptNo", "Receipt No", "text", "left"),
                col("customer", "Customer", "text", "left"),
                col("currency", "Currency", "text", "center"),
                col("amount", "Amount", "money", "right"),
                col("rate", "Rate to TZS", "number", "right"),
                col("amountTzs", "Amount (TZS)", "money", "right"),
                col("by", "Received by", "text", "left"));

        List<List<Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> byCur = new LinkedHashMap<>();
        for (Payment p : list) {
            rows.add(row(date(p.getPaidAt()), p.getReceiptNo(),
                    p.getCustomer() == null ? "" : p.getCustomer().getName(),
                    p.getCurrency(), p.getAmount(), p.getRateToTzs(), p.getAmountTzs(),
                    p.getReceivedBy() == null ? "" : p.getReceivedBy().getFullName()));
            total = total.add(nz(p.getAmountTzs()));
            byCur.merge(s(p.getCurrency()), nz(p.getAmount()), BigDecimal::add);
        }
        long voided = count("select count(p) from Payment p where p.receiptVoided = true "
                + "and p.paidAt >= :st and p.paidAt < :en", "st", c.start(), "en", c.end());

        List<Stat> stats = new ArrayList<>();
        stats.add(new Stat("Total received", money(total)));
        stats.add(new Stat("Receipts issued", String.valueOf(list.size())));
        stats.add(new Stat("Average receipt", money(list.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(list.size()), 2, java.math.RoundingMode.HALF_UP))));
        stats.add(new Stat("Voided receipts (excluded)", String.valueOf(voided)));
        for (Map.Entry<String, BigDecimal> e : byCur.entrySet())
            if (!e.getKey().isEmpty()) stats.add(new Stat("Paid in " + e.getKey(), num(e.getValue())));
        return make(c, "revenue", "Revenue (Payments Received)", cols, rows, stats);
    }

    /* ======================= EXPENSES ======================= */
    private ReportData expenses(Ctx c) {
        List<Expense> list = expenseList(c);
        List<Col> cols = List.of(
                col("date", "Date", "date", "left"),
                col("no", "Expense No", "text", "left"),
                col("category", "Category", "text", "left"),
                col("description", "Description", "text", "left"),
                col("paidTo", "Paid to", "text", "left"),
                col("reference", "Reference", "text", "left"),
                col("amount", "Amount (TZS)", "money", "right"),
                col("by", "Recorded by", "text", "left"));

        List<List<Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> byCat = new HashMap<>();
        for (Expense e : list) {
            rows.add(row(s(e.getDate()), e.getExpenseNo(), e.getCategory(), e.getDescription(), e.getPaidTo(),
                    e.getReference(), e.getAmount(), e.getCreatedBy() == null ? "" : e.getCreatedBy().getFullName()));
            total = total.add(nz(e.getAmount()));
            byCat.merge(s(e.getCategory()), nz(e.getAmount()), BigDecimal::add);
        }

        List<Stat> stats = new ArrayList<>();
        stats.add(new Stat("Total expenses", money(total)));
        stats.add(new Stat("Number of expenses", String.valueOf(list.size())));
        byCat.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(4)
                .forEach(e -> stats.add(new Stat(e.getKey(), money(e.getValue()))));
        return make(c, "expenses", "Expenses Report", cols, rows, stats);
    }

    /* ======================= DEBTS ======================= */
    private ReportData debts(Ctx c) {
        List<Invoice> all = em.createQuery("select i from Invoice i where i.voided = false and i.issueDate <= :t "
                + "order by i.dueDate, i.id", Invoice.class).setParameter("t", c.to()).getResultList();

        List<Col> cols = List.of(
                col("no", "Invoice No", "text", "left"),
                col("customer", "Customer", "text", "left"),
                col("issued", "Issued", "date", "left"),
                col("due", "Due date", "date", "left"),
                col("total", "Total (TZS)", "money", "right"),
                col("paid", "Paid (TZS)", "money", "right"),
                col("balance", "Balance (TZS)", "money", "right"),
                col("overdue", "Days overdue", "number", "right"),
                col("status", "Status", "text", "center"));

        List<List<Object>> rows = new ArrayList<>();
        BigDecimal debt = BigDecimal.ZERO;
        BigDecimal overdueAmt = BigDecimal.ZERO;
        Set<Long> debtors = new HashSet<>();
        for (Invoice i : all) {
            BigDecimal bal = i.getBalance();
            if (bal.signum() <= 0) continue;
            long late = i.getDueDate() != null && i.getDueDate().isBefore(c.today())
                    ? ChronoUnit.DAYS.between(i.getDueDate(), c.today()) : 0;
            rows.add(row(i.getInvoiceNo(), i.getCustomer() == null ? "" : i.getCustomer().getName(),
                    s(i.getIssueDate()), s(i.getDueDate()), i.getTotal(), i.getPaid(), bal, late, label(i.getStatus())));
            debt = debt.add(bal);
            if (late > 0) overdueAmt = overdueAmt.add(bal);
            if (i.getCustomer() != null) debtors.add(i.getCustomer().getId());
        }

        List<Stat> stats = List.of(
                new Stat("Total outstanding", money(debt)),
                new Stat("Overdue amount", money(overdueAmt)),
                new Stat("Unpaid invoices", String.valueOf(rows.size())),
                new Stat("Customers with debt", String.valueOf(debtors.size())),
                new Stat("Note", "Balances are current as of " + c.today()));
        return make(c, "debts", "Debts Report (Outstanding Invoices)", cols, rows, stats);
    }

    /* ======================= CUSTOMERS ======================= */
    private ReportData customers(Ctx c) {
        List<Customer> all = em.createQuery("select x from Customer x order by x.name", Customer.class).getResultList();

        Map<Long, BigDecimal> invoiced = sums("select i.customer.id, sum(i.total) from Invoice i where i.voided = false "
                + "and i.issueDate >= :f and i.issueDate <= :t group by i.customer.id", "f", c.from(), "t", c.to());
        Map<Long, BigDecimal> paid = sums("select p.customer.id, sum(p.amountTzs) from Payment p where p.receiptVoided = false "
                + "and p.paidAt >= :st and p.paidAt < :en group by p.customer.id", "st", c.start(), "en", c.end());
        Map<Long, BigDecimal> balance = sums("select i.customer.id, sum(i.total - i.purchasePaid - i.shippingPaid) "
                + "from Invoice i where i.voided = false group by i.customer.id");
        Map<Long, BigDecimal> ships = sums("select sh.customer.id, count(sh) from Shipment sh "
                + "where sh.createdAt >= :st and sh.createdAt < :en group by sh.customer.id", "st", c.start(), "en", c.end());

        List<Col> cols = List.of(
                col("no", "Customer No", "text", "left"),
                col("name", "Name", "text", "left"),
                col("phone", "Phone", "text", "left"),
                col("email", "Email", "text", "left"),
                col("registered", "Registered", "date", "left"),
                col("invoiced", "Invoiced (TZS)", "money", "right"),
                col("paid", "Paid (TZS)", "money", "right"),
                col("ships", "Shipments", "number", "right"),
                col("balance", "Outstanding (TZS)", "money", "right"));

        List<List<Object>> rows = new ArrayList<>();
        long newCount = 0;
        long activeCount = 0;
        long withDebt = 0;
        for (Customer x : all) {
            BigDecimal inv = invoiced.getOrDefault(x.getId(), BigDecimal.ZERO);
            BigDecimal pd = paid.getOrDefault(x.getId(), BigDecimal.ZERO);
            BigDecimal bal = balance.getOrDefault(x.getId(), BigDecimal.ZERO);
            long sh = ships.getOrDefault(x.getId(), BigDecimal.ZERO).longValue();
            boolean isNew = x.getCreatedAt() != null && !x.getCreatedAt().isBefore(c.start()) && x.getCreatedAt().isBefore(c.end());
            boolean active = inv.signum() > 0 || pd.signum() > 0 || sh > 0;
            if (bal.signum() > 0) withDebt++;
            if (isNew) newCount++;
            if (active) activeCount++;
            if (!isNew && !active) continue;
            rows.add(row(x.getCustomerNo(), x.getName(), x.getPhone(), x.getEmail(), date(x.getCreatedAt()), inv, pd, sh, bal));
        }

        List<Stat> stats = List.of(
                new Stat("Total customers", String.valueOf(all.size())),
                new Stat("New in period", String.valueOf(newCount)),
                new Stat("Active in period", String.valueOf(activeCount)),
                new Stat("With outstanding balance (now)", String.valueOf(withDebt)));
        return make(c, "customers", "Customers Report", cols, rows, stats);
    }

    /* ======================= EMPLOYEES ======================= */
    private ReportData employees(Ctx c) {
        List<User> users = em.createQuery("select u from User u order by u.fullName", User.class).getResultList();
        Object[] w = {"st", c.start(), "en", c.end()};

        Map<Long, BigDecimal> invCount = sums("select i.createdBy.id, count(i) from Invoice i where i.voided = false "
                + "and i.createdAt >= :st and i.createdAt < :en group by i.createdBy.id", w);
        Map<Long, BigDecimal> rcCount = sums("select p.receivedBy.id, count(p) from Payment p where p.receiptVoided = false "
                + "and p.paidAt >= :st and p.paidAt < :en group by p.receivedBy.id", w);
        Map<Long, BigDecimal> rcSum = sums("select p.receivedBy.id, sum(p.amountTzs) from Payment p where p.receiptVoided = false "
                + "and p.paidAt >= :st and p.paidAt < :en group by p.receivedBy.id", w);
        Map<Long, BigDecimal> shCount = sums("select sh.createdBy.id, count(sh) from Shipment sh "
                + "where sh.createdAt >= :st and sh.createdAt < :en group by sh.createdBy.id", w);
        Map<Long, BigDecimal> actCount = sums("select a.userId, count(a) from ActivityLog a "
                + "where a.createdAt >= :st and a.createdAt < :en group by a.userId", w);

        List<Col> cols = List.of(
                col("name", "Name", "text", "left"),
                col("role", "Role", "text", "left"),
                col("email", "Email", "text", "left"),
                col("status", "Status", "text", "center"),
                col("invoices", "Invoices created", "number", "right"),
                col("receipts", "Receipts issued", "number", "right"),
                col("received", "Amount received (TZS)", "money", "right"),
                col("shipments", "Shipments registered", "number", "right"),
                col("actions", "Actions logged", "number", "right"),
                col("login", "Last login", "date", "left"));

        List<List<Object>> rows = new ArrayList<>();
        BigDecimal received = BigDecimal.ZERO;
        long actions = 0;
        long active = 0;
        for (User u : users) {
            // Meneja haoni shughuli za Msimamizi (Admin)
            if (c.me().getRole() == Role.MANAGER && u.getRole() == Role.ADMIN) continue;
            BigDecimal rs = rcSum.getOrDefault(u.getId(), BigDecimal.ZERO);
            long ac = actCount.getOrDefault(u.getId(), BigDecimal.ZERO).longValue();
            rows.add(row(u.getFullName(), label(u.getRole()), u.getEmail(), u.isActive() ? "Active" : "Inactive",
                    invCount.getOrDefault(u.getId(), BigDecimal.ZERO).longValue(),
                    rcCount.getOrDefault(u.getId(), BigDecimal.ZERO).longValue(), rs,
                    shCount.getOrDefault(u.getId(), BigDecimal.ZERO).longValue(), ac, date(u.getLastLogin())));
            received = received.add(rs);
            actions += ac;
            if (u.isActive()) active++;
        }

        List<Stat> stats = List.of(
                new Stat("Staff listed", String.valueOf(rows.size())),
                new Stat("Active accounts", String.valueOf(active)),
                new Stat("Amount received by staff", money(received)),
                new Stat("Actions logged", String.valueOf(actions)));
        return make(c, "employees", "Employees Report", cols, rows, stats);
    }

    /* ======================= SHIPMENTS ======================= */
    private ReportData shipments(Ctx c) {
        List<Shipment> list = em.createQuery("select sh from Shipment sh where sh.createdAt >= :st and sh.createdAt < :en "
                        + "order by sh.createdAt, sh.id", Shipment.class)
                .setParameter("st", c.start()).setParameter("en", c.end()).getResultList();

        List<Col> cols = List.of(
                col("cargo", "Cargo No", "text", "left"),
                col("tracking", "Tracking No", "text", "left"),
                col("customer", "Customer", "text", "left"),
                col("mode", "Mode", "text", "center"),
                col("route", "Route", "text", "left"),
                col("packages", "Packages", "number", "right"),
                col("weight", "Weight (kg)", "number", "right"),
                col("chargeable", "Chargeable (kg)", "number", "right"),
                col("charge", "Charge (TZS)", "money", "right"),
                col("status", "Status", "text", "center"),
                col("registered", "Registered", "date", "left"),
                col("expected", "Expected arrival", "date", "left"));

        List<List<Object>> rows = new ArrayList<>();
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal chargeable = BigDecimal.ZERO;
        BigDecimal charges = BigDecimal.ZERO;
        Map<String, Long> byStatus = new LinkedHashMap<>();
        Map<String, Long> byMode = new LinkedHashMap<>();
        for (Shipment sh : list) {
            String route = (s(sh.getOrigin()) + " to " + s(sh.getDestination())).trim();
            rows.add(row(sh.getCargoNo(), sh.getTrackingNo(), sh.getCustomer() == null ? "" : sh.getCustomer().getName(),
                    label(sh.getMode()), route, sh.getPackages(), sh.getWeightKg(), sh.getChargeableKg(), sh.getChargeTzs(),
                    label(sh.getStatus()), date(sh.getCreatedAt()), s(sh.getExpectedArrival())));
            weight = weight.add(nz(sh.getWeightKg()));
            chargeable = chargeable.add(nz(sh.getChargeableKg()));
            charges = charges.add(nz(sh.getChargeTzs()));
            byStatus.merge(label(sh.getStatus()), 1L, Long::sum);
            byMode.merge(label(sh.getMode()), 1L, Long::sum);
        }

        List<Stat> stats = new ArrayList<>();
        stats.add(new Stat("Shipments registered", String.valueOf(list.size())));
        stats.add(new Stat("Total weight (kg)", num(weight)));
        stats.add(new Stat("Chargeable weight (kg)", num(chargeable)));
        stats.add(new Stat("Total charges", money(charges)));
        byMode.forEach((k, v) -> stats.add(new Stat("Mode: " + k, String.valueOf(v))));
        byStatus.forEach((k, v) -> stats.add(new Stat("Status: " + k, String.valueOf(v))));
        return make(c, "shipments", "Shipments Report", cols, rows, stats);
    }

    /* ======================= Data ya pamoja ======================= */
    private List<Payment> payments(Ctx c) {
        return em.createQuery("select p from Payment p where p.receiptVoided = false and p.paidAt >= :st "
                        + "and p.paidAt < :en order by p.paidAt, p.id", Payment.class)
                .setParameter("st", c.start()).setParameter("en", c.end()).getResultList();
    }

    private List<Expense> expenseList(Ctx c) {
        return em.createQuery("select e from Expense e where e.voided = false and e.date >= :f and e.date <= :t "
                        + "order by e.date, e.id", Expense.class)
                .setParameter("f", c.from()).setParameter("t", c.to()).getResultList();
    }

    /* ======================= Vifaa vidogo ======================= */
    private ReportData make(Ctx c, String type, String title, List<Col> cols, List<List<Object>> rows, List<Stat> stats) {
        String pn = c.periodName().substring(0, 1).toUpperCase(Locale.ROOT) + c.periodName().substring(1);
        return new ReportData(type, title, pn + ": " + c.from() + " to " + c.to(), c.from().toString(), c.to().toString(),
                cols, rows, stats, c.me().getFullName(),
                LocalDateTime.now(settings.zone()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    private static Col col(String key, String label, String type, String align) { return new Col(key, label, type, align); }

    private static List<Object> row(Object... v) { return new ArrayList<>(Arrays.asList(v)); }

    private static String key(LocalDate d, boolean monthly) { return monthly ? YearMonth.from(d).toString() : d.toString(); }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static String s(Object v) { return v == null ? "" : v.toString(); }

    private static String date(LocalDateTime d) { return d == null ? "" : d.toLocalDate().toString(); }

    private static String label(Enum<?> e) { return e == null ? "" : e.name().replace('_', ' '); }

    private static String money(BigDecimal v) { return "TZS " + String.format(Locale.US, "%,.2f", nz(v)); }

    private static String num(BigDecimal v) { return String.format(Locale.US, "%,.2f", nz(v)); }

    private Query query(String jpql, Object... kv) {
        Query q = em.createQuery(jpql);
        for (int i = 0; i + 1 < kv.length; i += 2) q.setParameter((String) kv[i], kv[i + 1]);
        return q;
    }

    private BigDecimal scalar(String jpql, Object... kv) {
        Object r = query(jpql, kv).getSingleResult();
        return r == null ? BigDecimal.ZERO : new BigDecimal(r.toString());
    }

    private long count(String jpql, Object... kv) {
        Object r = query(jpql, kv).getSingleResult();
        return r == null ? 0 : ((Number) r).longValue();
    }

    /** Matokeo ya (id, jumla) kuwa Map. */
    private Map<Long, BigDecimal> sums(String jpql, Object... kv) {
        Map<Long, BigDecimal> m = new HashMap<>();
        for (Object o : query(jpql, kv).getResultList()) {
            Object[] r = (Object[]) o;
            if (r[0] != null) m.put(((Number) r[0]).longValue(), r[1] == null ? BigDecimal.ZERO : new BigDecimal(r[1].toString()));
        }
        return m;
    }
}