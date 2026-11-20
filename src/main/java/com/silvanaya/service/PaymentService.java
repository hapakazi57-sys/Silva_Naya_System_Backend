package com.silvanaya.service;

import com.silvanaya.dto.Dtos.*;
import com.silvanaya.model.*;
import com.silvanaya.model.Enums.PaymentPart;
import com.silvanaya.repository.*;
import com.silvanaya.util.ApiException;
import com.silvanaya.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service @RequiredArgsConstructor @Transactional
public class PaymentService {
    private static final BigDecimal TOL = new BigDecimal("0.01");
    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final CustomerRepository customers;
    private final PricingService pricing;
    private final AuditService audit;
    private final SettingsService settings;

    public Payment create(PaymentReq r) {
        Customer c = customers.findById(r.customerId()).orElseThrow(() -> ApiException.notFound("Customer"));
        BigDecimal rate = pricing.rateToTzs(r.currency().toUpperCase(), LocalDate.now());
        BigDecimal amountTzs = r.amount().multiply(rate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal sum = r.allocations().stream().map(AllocReq::amountTzs).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.subtract(amountTzs).compareTo(TOL) > 0)
            throw ApiException.bad("Total Allocation (TZS " + sum + ") Exceeds the Amount Paid. (TZS " + amountTzs + ")");

        Payment p = new Payment();
        p.setCustomer(c); p.setAmount(r.amount()); p.setCurrency(r.currency().toUpperCase());
        p.setRateToTzs(rate); p.setAmountTzs(amountTzs); p.setNote(r.note());
        p.setReceivedBy(CurrentUser.require());

        for (AllocReq a : r.allocations()) {
            Invoice inv = invoices.findById(a.invoiceId()).orElseThrow(() -> ApiException.notFound("Invoice"));
            if (inv.isVoided()) throw ApiException.bad("Invoice " + inv.getInvoiceNo() + " Cancelled");
            if (!inv.getCustomer().getId().equals(c.getId()))
                throw ApiException.bad("Invoice " + inv.getInvoiceNo() + " Does not belong to this customer.");
            BigDecimal outstanding = a.part() == PaymentPart.PURCHASE ? inv.getPurchaseBalance() : inv.getShippingBalance();
            if (a.amountTzs().subtract(outstanding).compareTo(TOL) > 0)
                throw ApiException.bad("Amount exceeds the outstanding invoice balance." + inv.getInvoiceNo() + " (Remained TZS " + outstanding + ")");
            if (a.part() == PaymentPart.PURCHASE) inv.setPurchasePaid(inv.getPurchasePaid().add(a.amountTzs()));
            else inv.setShippingPaid(inv.getShippingPaid().add(a.amountTzs()));
            inv.refreshStatus();
            PaymentAllocation pa = new PaymentAllocation();
            pa.setInvoice(inv); pa.setPart(a.part()); pa.setAmountTzs(a.amountTzs());
            p.getAllocations().add(pa);
        }
        payments.save(p);
        p.setReceiptNo(settings.number("REC", p.getId()));
        audit.log("CREATE", "Payment", p.getReceiptNo() + " - " + c.getName() + " - " + p.getCurrency() + " " + p.getAmount());
        return p;
    }


    public Payment voidReceipt(Long id, String reason) {
        Payment p = payments.findById(id).orElseThrow(() -> ApiException.notFound("Payment"));
        if (p.isReceiptVoided()) throw ApiException.bad("This receipt has already been cancelled.");
        for (PaymentAllocation a : p.getAllocations()) {
            Invoice inv = a.getInvoice();
            if (a.getPart() == PaymentPart.PURCHASE) inv.setPurchasePaid(inv.getPurchasePaid().subtract(a.getAmountTzs()));
            else inv.setShippingPaid(inv.getShippingPaid().subtract(a.getAmountTzs()));
            inv.refreshStatus();
        }
        p.setReceiptVoided(true); p.setVoidReason(reason); p.setVoidedBy(CurrentUser.require().getFullName());
        audit.log("VOID", "Receipt", p.getReceiptNo() + " - " + reason);
        return p;
    }
}
