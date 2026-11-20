package com.silvanaya.model;

import com.silvanaya.model.Enums.PaymentPart;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "payment_allocations") @Getter @Setter @NoArgsConstructor
public class PaymentAllocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne private Invoice invoice;
    @Enumerated(EnumType.STRING) private PaymentPart part;
    private BigDecimal amountTzs;
}
