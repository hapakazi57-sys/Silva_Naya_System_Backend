package com.silvanaya.service;

import com.silvanaya.dto.Dtos.InvoiceReq;
import com.silvanaya.model.*;
import com.silvanaya.model.Enums.*;
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
public class InvoiceService {
    private final InvoiceRepository invoices;
    private final CustomerRepository customers;
    private final PurchaseOrderRepository orders;
    private final ShipmentRepository shipments;
    private final PricingService pricing;
    private final AuditService audit;
    private final SettingsService settings;

    public Invoice create(InvoiceReq r) {
        if (r.orderId() == null && r.shipmentId() == null)
            throw ApiException.bad("Select a Purchase Order, a Shipment, or Both.");
        Customer c = customers.findById(r.customerId()).orElseThrow(() -> ApiException.notFound("Customer"));
        Invoice inv = new Invoice();
        inv.setCustomer(c);
        inv.setCreatedBy(CurrentUser.require());
        inv.setIssueDate(LocalDate.now());
        inv.setDueDate(r.dueDate() != null ? r.dueDate() : LocalDate.now().plusDays(7));

        if (r.orderId() != null) {
            PurchaseOrder o = orders.findById(r.orderId()).orElseThrow(() -> ApiException.notFound("Oda"));
            if (!o.getCustomer().getId().equals(c.getId())) throw ApiException.bad("This order does not belong to the selected customer.");
            if (o.getStatus() != OrderStatus.APPROVED) throw ApiException.bad("The order must first be approved by the Manager.");
            if (invoices.existsByPurchaseOrderIdAndVoidedFalse(o.getId())) throw ApiException.bad("An invoice has already been generated for this order.");
            BigDecimal base = o.getActualCost() != null ? o.getActualCost() : o.getTotalAmount();
            inv.setPurchaseOrder(o);
            inv.setPurchaseAmount(base.multiply(pricing.rateToTzs(o.getCurrency(), LocalDate.now())).setScale(2, RoundingMode.HALF_UP));
        }
        if (r.shipmentId() != null) {
            Shipment s = shipments.findById(r.shipmentId()).orElseThrow(() -> ApiException.notFound("Shipment"));
            if (!s.getCustomer().getId().equals(c.getId())) throw ApiException.bad("This shipment does not belong to the selected customer.");
            if (invoices.existsByShipmentIdAndVoidedFalse(s.getId())) throw ApiException.bad("An invoice has already been generated for this shipment.");
            inv.setShipment(s);
            inv.setShippingAmount(s.getChargeTzs());
        }
        inv.setType(inv.getPurchaseOrder() != null && inv.getShipment() != null ? InvoiceType.COMBINED
                : inv.getPurchaseOrder() != null ? InvoiceType.PURCHASE : InvoiceType.SHIPPING);
        inv.refreshStatus();
        invoices.save(inv);
        inv.setInvoiceNo(settings.number("INV", inv.getId()));
        audit.log("CREATE", "Invoice", inv.getInvoiceNo() + " - " + c.getName() + " - TZS " + inv.getTotal());
        return inv;
    }


    public Invoice voidInvoice(Long id, String reason) {
        Invoice i = invoices.findById(id).orElseThrow(() -> ApiException.notFound("Invoice"));
        if (i.isVoided()) throw ApiException.bad("This invoice has already been cancelled.");
        if (i.getPaid().signum() > 0) throw ApiException.bad("This invoice has recorded payments. Please cancel the payment receipts first.");
        i.setVoided(true); i.setVoidReason(reason);
        audit.log("VOID", "Invoice", i.getInvoiceNo() + " - " + reason);
        return i;
    }
}