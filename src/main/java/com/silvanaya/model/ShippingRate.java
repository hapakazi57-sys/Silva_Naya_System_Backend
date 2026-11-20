package com.silvanaya.model;

import com.silvanaya.model.Enums.GoodsType;
import com.silvanaya.model.Enums.TransportMode;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "shipping_rates") @Getter @Setter @NoArgsConstructor
public class ShippingRate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) private TransportMode mode;
    @Enumerated(EnumType.STRING) private GoodsType goodsType = GoodsType.NORMAL;   // NORMAL au HK
    private String originCountry = "ALL";          // CHINA, DUBAI au ALL
    private BigDecimal ratePerKg = BigDecimal.ZERO;
    private BigDecimal ratePerCbm = BigDecimal.ZERO;
    private BigDecimal minCharge = BigDecimal.ZERO;
    private int volumetricDivisor = 6000;           // kwa usafirishaji wa anga
    private String currency = "USD";
    private LocalDate effectiveFrom = LocalDate.now();
    private String createdBy;
}