package com.silvanaya.service;

import com.silvanaya.model.*;
import com.silvanaya.model.Enums.GoodsType;
import com.silvanaya.model.Enums.TransportMode;
import com.silvanaya.repository.*;
import com.silvanaya.util.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;


@Service @RequiredArgsConstructor
public class PricingService {
    private final ShippingRateRepository rates;
    private final ExchangeRateRepository fx;

    public record Charge(BigDecimal cbm, BigDecimal chargeableKg, BigDecimal amount, String currency, BigDecimal amountTzs) {}

    public BigDecimal rateToTzs(String cur, LocalDate date) {
        if ("TZS".equalsIgnoreCase(cur)) return BigDecimal.ONE;
        return fx.findAll().stream()
                .filter(r -> r.getCurrency().equalsIgnoreCase(cur) && !r.getEffectiveFrom().isAfter(date))
                .max(Comparator.comparing((ExchangeRate r) -> r.getEffectiveFrom()).thenComparing(ExchangeRate::getId))
                .map(ExchangeRate::getRateToTzs)
                .orElseThrow(() -> ApiException.bad("Exchange Rate " + cur + " Not Set."));
    }

    private static BigDecimal n(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }

    /** Rate za zamani zisizo na aina huchukuliwa kama NORMAL. */
    private static GoodsType goodsOf(ShippingRate r) { return r.getGoodsType() == null ? GoodsType.NORMAL : r.getGoodsType(); }

    public Charge calc(TransportMode mode, GoodsType goodsType, String country, Integer packages, BigDecimal kg,
                       BigDecimal l, BigDecimal w, BigDecimal h, LocalDate date) {
        GoodsType goods = goodsType == null ? GoodsType.NORMAL : goodsType;
        ShippingRate r = rates.findAll().stream()
                .filter(x -> x.getMode() == mode && goodsOf(x) == goods && !x.getEffectiveFrom().isAfter(date)
                        && (x.getOriginCountry().equalsIgnoreCase(country) || x.getOriginCountry().equalsIgnoreCase("ALL")))
                .max(Comparator.comparing((ShippingRate x) -> x.getEffectiveFrom())
                        .thenComparingInt(x -> x.getOriginCountry().equalsIgnoreCase("ALL") ? 0 : 1)
                        .thenComparing(ShippingRate::getId))
                .orElseThrow(() -> ApiException.bad("No shipping rate has been configured for this method. (" + mode + " / " + goods + ")"));

        BigDecimal pieces = BigDecimal.valueOf(packages == null || packages < 1 ? 1 : packages);
        BigDecimal volume = n(l).multiply(n(w)).multiply(n(h)).multiply(pieces);          // cm3
        BigDecimal cbm = volume.divide(BigDecimal.valueOf(1_000_000), 4, RoundingMode.HALF_UP);
        BigDecimal volKg = volume.divide(BigDecimal.valueOf(r.getVolumetricDivisor()), 2, RoundingMode.HALF_UP);

        BigDecimal chargeable, amount;
        if (mode == TransportMode.AIR) {
            chargeable = n(kg).max(volKg);
            amount = chargeable.multiply(r.getRatePerKg());
        } else {
            chargeable = n(kg);
            amount = cbm.multiply(r.getRatePerCbm());
        }
        amount = amount.max(r.getMinCharge()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tzs = amount.multiply(rateToTzs(r.getCurrency(), date)).setScale(2, RoundingMode.HALF_UP);
        return new Charge(cbm, chargeable.setScale(2, RoundingMode.HALF_UP), amount, r.getCurrency(), tzs);
    }
}