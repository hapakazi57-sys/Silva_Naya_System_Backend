package com.silvanaya.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "expenses") @Getter @Setter @NoArgsConstructor
public class Expense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true) private String expenseNo;
    private LocalDate date = LocalDate.now();
    private String category;
    @Column(length = 500) private String description;
    private BigDecimal amount = BigDecimal.ZERO;   // TZS
    private String paidTo;
    private String reference;
    private boolean voided;
    private String voidReason;
    @ManyToOne private User createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();
}