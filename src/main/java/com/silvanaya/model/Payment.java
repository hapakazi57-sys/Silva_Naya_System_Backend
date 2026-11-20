package com.silvanaya.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "payments") @Getter @Setter @NoArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true) private String receiptNo;
    @ManyToOne private Customer customer;
    private BigDecimal amount;
    private String currency;
    private BigDecimal rateToTzs = BigDecimal.ONE;
    private BigDecimal amountTzs;
    private String note;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_id")
    private List<PaymentAllocation> allocations = new ArrayList<>();
    @ManyToOne private User receivedBy;
    private LocalDateTime paidAt = LocalDateTime.now();
    private boolean receiptVoided;
    private String voidReason;
    private String voidedBy;
}
