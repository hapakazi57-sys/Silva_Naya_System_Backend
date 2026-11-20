package com.silvanaya.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "exchange_rates") @Getter @Setter @NoArgsConstructor
public class ExchangeRate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String currency;                 // USD, CNY (TZS = 1)
    private BigDecimal rateToTzs;
    private LocalDate effectiveFrom = LocalDate.now();
    private String createdBy;
}
