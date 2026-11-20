package com.silvanaya.service;

import com.silvanaya.dto.Dtos.*;
import com.silvanaya.model.*;
import com.silvanaya.model.Enums.GoodsType;
import com.silvanaya.model.Enums.ShipmentStatus;
import com.silvanaya.repository.*;
import com.silvanaya.util.ApiException;
import com.silvanaya.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor @Transactional
public class ShipmentService {
    private final ShipmentRepository shipments;
    private final ShipmentHistoryRepository history;
    private final CustomerRepository customers;
    private final PurchaseOrderRepository orders;
    private final InvoiceRepository invoices;
    private final PricingService pricing;
    private final AuditService audit;

    public Shipment create(ShipmentReq r) {
        Shipment s = new Shipment();
        s.setCreatedBy(CurrentUser.require());
        fill(s, r, true);
        s.setDestination(r.destination() == null || r.destination().isBlank() ? "Tanzania" : r.destination());
        shipments.save(s);
        s.setCargoNo(CurrentUser.number("SH", s.getId()));
        addHistory(s, "Shipment registered");
        audit.log("CREATE", "Shipment", s.getCargoNo() + " - " + s.getCustomer().getName());
        return s;
    }

    public Shipment update(Long id, ShipmentReq r) {
        Shipment s = get(id);
        boolean locked = invoices.existsByShipmentIdAndVoidedFalse(id);
        fill(s, r, !locked);   // if an invoice exists, measurements, goods type and charges cannot be changed
        audit.log("UPDATE", "Shipment", s.getCargoNo() + (locked ? " (measurements locked because an invoice exists)" : ""));
        return s;
    }

    private void fill(Shipment s, ShipmentReq r, boolean measurements) {
        s.setCustomer(customers.findById(r.customerId()).orElseThrow(() -> ApiException.notFound("Customer")));
        s.setPurchaseOrder(r.orderId() == null ? null : orders.findById(r.orderId()).orElseThrow(() -> ApiException.notFound("Order")));
        s.setTrackingNo(r.trackingNo());
        s.setDescription(r.description());
        s.setOrigin(r.origin());
        if (r.destination() != null && !r.destination().isBlank()) s.setDestination(r.destination());
        s.setDepartureDate(r.departureDate());
        s.setExpectedArrival(r.expectedArrival());
        if (!measurements) return;
        GoodsType goods = r.goodsType() == null ? GoodsType.NORMAL : r.goodsType();
        s.setMode(r.mode());
        s.setGoodsType(goods);
        s.setOriginCountry(r.originCountry());
        s.setPackages(r.packages() == null || r.packages() < 1 ? 1 : r.packages());
        s.setWeightKg(nz(r.weightKg())); s.setLengthCm(nz(r.lengthCm()));
        s.setWidthCm(nz(r.widthCm())); s.setHeightCm(nz(r.heightCm()));
        var c = pricing.calc(r.mode(), goods, r.originCountry(), s.getPackages(), s.getWeightKg(),
                s.getLengthCm(), s.getWidthCm(), s.getHeightCm(), LocalDate.now());
        s.setCbm(c.cbm()); s.setChargeableKg(c.chargeableKg());
        s.setChargeAmount(c.amount()); s.setChargeCurrency(c.currency()); s.setChargeTzs(c.amountTzs());
    }

    public Shipment changeStatus(Long id, StatusReq r) {
        if (r.status() == ShipmentStatus.DELIVERED)
            throw ApiException.bad("Use the 'Deliver cargo' button to hand the shipment over to the customer");
        Shipment s = get(id);
        if (s.getStatus() == ShipmentStatus.DELIVERED)
            throw ApiException.bad("This shipment has already been delivered, so its status can no longer be changed");
        s.setStatus(r.status());
        if (r.status() == ShipmentStatus.ARRIVED && s.getActualArrival() == null) s.setActualArrival(LocalDate.now());
        addHistory(s, r.note());
        audit.log("STATUS", "Shipment", s.getCargoNo() + " -> " + r.status());
        return s;
    }

    public Shipment deliver(Long id, DeliverReq r) {
        Shipment s = get(id);
        if (s.getStatus() != ShipmentStatus.ARRIVED && s.getStatus() != ShipmentStatus.READY_FOR_PICKUP)
            throw ApiException.bad("The shipment must have arrived in Tanzania before it can be delivered");
        s.setStatus(ShipmentStatus.DELIVERED);
        s.setReceiverName(r.receiverName()); s.setReceiverPhone(r.receiverPhone());
        s.setDeliveryNote(r.note()); s.setDeliveredAt(LocalDateTime.now());
        addHistory(s, "Delivered to: " + r.receiverName() + " (" + r.receiverPhone() + ")");
        audit.log("DELIVER", "Shipment", s.getCargoNo() + " -> " + r.receiverName());
        return s;
    }

    public Shipment get(Long id) { return shipments.findById(id).orElseThrow(() -> ApiException.notFound("Shipment")); }

    private void addHistory(Shipment s, String note) {
        ShipmentHistory h = new ShipmentHistory();
        h.setShipmentId(s.getId()); h.setStatus(s.getStatus()); h.setNote(note);
        h.setChangedBy(CurrentUser.require().getFullName());
        history.save(h);
    }

    private static java.math.BigDecimal nz(java.math.BigDecimal x) { return x == null ? java.math.BigDecimal.ZERO : x; }
}