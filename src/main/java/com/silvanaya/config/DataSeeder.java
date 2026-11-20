package com.silvanaya.config;

import com.silvanaya.model.*;
import com.silvanaya.model.Enums.*;
import com.silvanaya.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Inaweka bei za mfano na viwango vya sarafu mara ya kwanza tu. Watumiaji wanatengenezwa kupitia API. */
@Component @RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final ShippingRateRepository rates;
    private final ExchangeRateRepository fx;

    @Override
    public void run(String... args) {
        if (rates.count() == 0) {
            rate(TransportMode.AIR, "ALL", "10", "0", "20");
            rate(TransportMode.SEA, "ALL", "0", "350", "50");
        }
        if (fx.count() == 0) {
            fx("USD", "2600");
            fx("CNY", "360");
        }
    }

    private void rate(TransportMode m, String country, String kg, String cbm, String min) {
        ShippingRate r = new ShippingRate();
        r.setMode(m); r.setOriginCountry(country);
        r.setRatePerKg(new BigDecimal(kg)); r.setRatePerCbm(new BigDecimal(cbm)); r.setMinCharge(new BigDecimal(min));
        r.setCreatedBy("system");
        rates.save(r);
    }

    private void fx(String cur, String rate) {
        ExchangeRate e = new ExchangeRate();
        e.setCurrency(cur); e.setRateToTzs(new BigDecimal(rate)); e.setCreatedBy("system");
        fx.save(e);
    }
}