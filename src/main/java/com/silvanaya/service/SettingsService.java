package com.silvanaya.service;

import com.silvanaya.model.CompanySettings;
import com.silvanaya.model.Enums.Role;
import com.silvanaya.model.User;
import com.silvanaya.repository.CompanySettingsRepository;
import com.silvanaya.util.ApiException;
import com.silvanaya.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

@Service @RequiredArgsConstructor @Transactional
public class SettingsService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern CCY = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern PREFIX = Pattern.compile("^[A-Z0-9]{1,6}$");
    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final int MAX_LOGO_CHARS = 900_000;

    private final CompanySettingsRepository repo;
    private final AuditService audit;

    public CompanySettings get() {
        return repo.findById(1L).orElseGet(() -> repo.save(new CompanySettings()));
    }

    public ZoneId zone() {
        try { return ZoneId.of(get().getTimezone()); }
        catch (Exception e) { return ZoneId.of("Africa/Dar_es_Salaam"); }
    }


    @EventListener(ApplicationReadyEvent.class)
    public void applyOnStartup() {
        TimeZone.setDefault(TimeZone.getTimeZone(zone()));
    }


    public String number(String type, Long id) {
        CompanySettings s = get();
        String prefix = switch (type) {
            case "INV" -> s.getInvoicePrefix();
            case "REC" -> s.getReceiptPrefix();
            case "TRX" -> s.getTransactionPrefix();
            default -> type;
        };
        String sep = s.getNumberSeparator() == null ? "" : s.getNumberSeparator();
        StringBuilder sb = new StringBuilder(prefix);
        if (s.isNumberIncludeYear()) sb.append(sep).append(LocalDate.now(zone()).getYear());
        sb.append(sep).append(String.format("%0" + s.getNumberPadding() + "d", id));
        return sb.toString();
    }


    public CompanySettings update(CompanySettings in) {
        User me = CurrentUser.require();
        if (me.getRole() != Role.ADMIN) throw ApiException.bad("Settings can only be modified by the System Administrator.");

        CompanySettings s = get();

        /* Kampuni */
        String name = clean(in.getCompanyName());
        if (name.isEmpty()) throw ApiException.bad("Please enter the company name.");
        if (name.length() > 150) throw ApiException.bad("Company name is too long.");
        String email = clean(in.getEmail());
        if (!email.isEmpty() && !EMAIL.matcher(email).matches()) throw ApiException.bad("Invalid email address.");
        String address = clean(in.getAddress());
        String phone = clean(in.getPhone());
        if (address.length() > 300) throw ApiException.bad("Address is too long.");
        if (phone.length() > 50) throw ApiException.bad("Phone number is too long.");

        String logo = in.getLogoData();
        if (logo == null || logo.isBlank()) logo = null;
        else if (!logo.startsWith("data:image/") || logo.length() > MAX_LOGO_CHARS)
            throw ApiException.bad("The logo must be an image file no larger than 500 KB.");

        /* Sarafu na saa */
        String base = clean(in.getBaseCurrency()).toUpperCase();
        if (!CCY.matcher(base).matches()) throw ApiException.bad("The base currency must be a 3-letter currency code (e.g., TZS).");
        Set<String> list = new LinkedHashSet<>();
        list.add(base);
        for (String c : clean(in.getCurrencies()).split(",")) {
            String u = c.trim().toUpperCase();
            if (u.isEmpty()) continue;
            if (!CCY.matcher(u).matches()) throw ApiException.bad("Invalid currency code: " + u);
            list.add(u);
        }
        String tz = clean(in.getTimezone());
        try { ZoneId.of(tz); } catch (Exception e) { throw ApiException.bad("Invalid system time zone."); }

        /* Muundo */
        String accent = clean(in.getAccentColor());
        if (!COLOR.matcher(accent).matches()) throw ApiException.bad("Invalid color code. Example:: #0b5fff");
        String invTitle = limit(in.getInvoiceTitle(), 40, "Invoice Title");
        String recTitle = limit(in.getReceiptTitle(), 40, "Receipt Title");
        if (invTitle.isEmpty() || recTitle.isEmpty()) throw ApiException.bad("Please enter the invoice and receipt titles.");
        String invTerms = limit(in.getInvoiceTerms(), 1000, "Invoice Terms");
        String invFooter = limit(in.getInvoiceFooter(), 500, "Invoice Footer");
        String recFooter = limit(in.getReceiptFooter(), 500, "Receipt Footer");

        /* Namba */
        String ip = clean(in.getInvoicePrefix()).toUpperCase();
        String rp = clean(in.getReceiptPrefix()).toUpperCase();
        String tp = clean(in.getTransactionPrefix()).toUpperCase();
        for (String p : List.of(ip, rp, tp))
            if (!PREFIX.matcher(p).matches()) throw ApiException.bad("Number prefix: 1–6 alphanumeric characters (e.g., INV).");
        if (ip.equals(rp) || ip.equals(tp) || rp.equals(tp))
            throw ApiException.bad("Invoice, receipt, and transaction prefixes must be unique.");
        String sep = in.getNumberSeparator() == null ? "" : in.getNumberSeparator();
        if (!List.of("-", "/", "_", "").contains(sep)) throw ApiException.bad("Invalid separator.");
        int pad = in.getNumberPadding();
        if (pad < 3 || pad > 10) throw ApiException.bad("The number of digits must be between 3 and 10.");

        s.setCompanyName(name);
        s.setLogoData(logo);
        s.setAddress(address);
        s.setPhone(phone);
        s.setEmail(email);
        s.setBaseCurrency(base);
        s.setCurrencies(String.join(",", list));
        s.setTimezone(tz);
        s.setAccentColor(accent);
        s.setInvoiceTitle(invTitle);
        s.setInvoiceShowLogo(in.isInvoiceShowLogo());
        s.setInvoiceTerms(invTerms);
        s.setInvoiceFooter(invFooter);
        s.setReceiptTitle(recTitle);
        s.setReceiptShowLogo(in.isReceiptShowLogo());
        s.setReceiptFooter(recFooter);
        s.setInvoicePrefix(ip);
        s.setReceiptPrefix(rp);
        s.setTransactionPrefix(tp);
        s.setNumberIncludeYear(in.isNumberIncludeYear());
        s.setNumberSeparator(sep);
        s.setNumberPadding(pad);
        s.setUpdatedAt(LocalDateTime.now());
        s.setUpdatedBy(me.getFullName());

        CompanySettings saved = repo.save(s);
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(tz)));
        audit.log("UPDATE", "Settings", "Company settings updated - " + name);
        return saved;
    }

    private static String clean(String v) { return v == null ? "" : v.trim(); }

    private static String limit(String v, int max, String label) {
        String t = clean(v);
        if (t.length() > max) throw ApiException.bad(label + " too long (above the letter " + max + ")");
        return t;
    }
}