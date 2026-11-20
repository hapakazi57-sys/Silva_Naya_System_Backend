package com.silvanaya.controller;

import com.silvanaya.dto.Dtos.*;
import com.silvanaya.model.*;
import com.silvanaya.model.Enums.GoodsType;
import com.silvanaya.repository.*;
import com.silvanaya.service.AuditService;
import com.silvanaya.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Bei mpya zinaongezwa kama rekodi mpya (historia inabaki); miamala ya zamani haiathiriwi. */
@RestController @RequestMapping("/api/rates") @RequiredArgsConstructor
public class RateController {
    private final ShippingRateRepository shipping;
    private final ExchangeRateRepository exchange;
    private final AuditService audit;

    @GetMapping("/shipping")
    public List<ShippingRate> shipping() { return shipping.findAll(Sort.by(Sort.Direction.DESC, "effectiveFrom", "id")); }

    @PostMapping("/shipping") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ShippingRate addShipping(@Valid @RequestBody RateReq r) {
        ShippingRate s = new ShippingRate();
        s.setMode(r.mode());
        s.setGoodsType(r.goodsType() == null ? GoodsType.NORMAL : r.goodsType());
        s.setOriginCountry(r.originCountry().toUpperCase());
        s.setRatePerKg(nz(r.ratePerKg())); s.setRatePerCbm(nz(r.ratePerCbm())); s.setMinCharge(nz(r.minCharge()));
        s.setVolumetricDivisor(r.volumetricDivisor() == null || r.volumetricDivisor() < 1 ? 6000 : r.volumetricDivisor());
        s.setCurrency(r.currency().toUpperCase());
        s.setEffectiveFrom(r.effectiveFrom() == null ? LocalDate.now() : r.effectiveFrom());
        s.setCreatedBy(CurrentUser.require().getFullName());
        shipping.save(s);
        audit.log("CREATE", "ShippingRate", s.getMode() + " " + s.getGoodsType() + " " + s.getOriginCountry() + " Starting " + s.getEffectiveFrom());
        return s;
    }

    @GetMapping("/exchange")
    public List<ExchangeRate> exchange() { return exchange.findAll(Sort.by(Sort.Direction.DESC, "effectiveFrom", "id")); }

    @PostMapping("/exchange") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ExchangeRate addExchange(@Valid @RequestBody ExchangeReq r) {
        ExchangeRate e = new ExchangeRate();
        e.setCurrency(r.currency().toUpperCase()); e.setRateToTzs(r.rateToTzs());
        e.setEffectiveFrom(r.effectiveFrom() == null ? LocalDate.now() : r.effectiveFrom());
        e.setCreatedBy(CurrentUser.require().getFullName());
        exchange.save(e);
        audit.log("CREATE", "ExchangeRate", e.getCurrency() + " = TZS " + e.getRateToTzs());
        return e;
    }

    private static BigDecimal nz(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
}