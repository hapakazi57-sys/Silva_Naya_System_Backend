package com.silvanaya.model;

import com.silvanaya.model.Enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "purchase_orders") @Getter @Setter @NoArgsConstructor
public class PurchaseOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true) private String orderNo;
    @ManyToOne private Customer customer;
    private String country;          // CHINA / DUBAI
    private String supplierName;
    private String currency = "TZS";
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal actualCost;
    @Enumerated(EnumType.STRING) private OrderStatus status = OrderStatus.DRAFT;
    private String note;
    private String managerNote;
    @ManyToOne private User createdBy;
    @ManyToOne private User approvedBy;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void recalc() {
        totalAmount = items.stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
