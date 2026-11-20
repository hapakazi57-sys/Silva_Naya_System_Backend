package com.silvanaya.controller;

import com.silvanaya.dto.Dtos.*;
import com.silvanaya.model.Enums.GoodsType;
import com.silvanaya.model.Shipment;
import com.silvanaya.repository.*;
import com.silvanaya.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/shipments") @RequiredArgsConstructor
public class ShipmentController {
    private final ShipmentRepository shipments;
    private final ShipmentHistoryRepository history;
    private final ShipmentService service;
    private final PricingService pricing;
    private final ModuleLockService locks;
    private final OptionsMapper opts;

    @GetMapping
    public List<Shipment> list(@RequestHeader(value = "X-Module-Tokens", required = false) String tk) {
        locks.require("shipments", tk);
        return shipments.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    /** Slim list for dropdowns in other forms (no lock). */
    @GetMapping("/options")
    public List<Map<String, Object>> options() {
        return shipments.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(s -> opts.pick(s, "id", "cargoNo", "chargeTzs")).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id,
                                   @RequestHeader(value = "X-Module-Tokens", required = false) String tk) {
        locks.require("shipments", tk);
        return Map.of("shipment", service.get(id), "history", history.findByShipmentIdOrderByChangedAtDesc(id));
    }

    @PostMapping("/calc")
    public PricingService.Charge calc(@Valid @RequestBody CalcReq r) {
        return pricing.calc(r.mode(), r.goodsType() == null ? GoodsType.NORMAL : r.goodsType(),
                r.originCountry(), r.packages(), r.weightKg(),
                r.lengthCm(), r.widthCm(), r.heightCm(), LocalDate.now());
    }

    @PostMapping
    public Shipment create(@Valid @RequestBody ShipmentReq r) { return service.create(r); }

    @PutMapping("/{id}")
    public Shipment update(@PathVariable Long id, @Valid @RequestBody ShipmentReq r) { return service.update(id, r); }

    @PostMapping("/{id}/status")
    public Shipment status(@PathVariable Long id, @Valid @RequestBody StatusReq r) { return service.changeStatus(id, r); }

    @PostMapping("/{id}/deliver")
    public Shipment deliver(@PathVariable Long id, @Valid @RequestBody DeliverReq r) { return service.deliver(id, r); }
}