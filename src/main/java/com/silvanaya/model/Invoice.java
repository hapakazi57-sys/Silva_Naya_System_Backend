package com.silvanaya.model;

import com.silvanaya.model.Enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "invoices") @Getter @Setter @NoArgsConstructor
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true) private String invoiceNo;
    @ManyToOne private Customer customer;
    @Enumerated(EnumType.STRING) private InvoiceType type;
    @ManyToOne private PurchaseOrder purchaseOrder;
    @ManyToOne private Shipment shipment;
    private LocalDate issueDate = LocalDate.now();
    private LocalDate dueDate;
    private BigDecimal purchaseAmount = BigDecimal.ZERO;   // TZS
    private BigDecimal shippingAmount = BigDecimal.ZERO;   // TZS
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal purchasePaid = BigDecimal.ZERO;
    private BigDecimal shippingPaid = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) private PayStatus status = PayStatus.UNPAID;
    private boolean voided;
    private String voidReason;
    @ManyToOne private User createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();

    public BigDecimal getPaid() { return purchasePaid.add(shippingPaid); }
    public BigDecimal getBalance() { return total.subtract(getPaid()); }
    public BigDecimal getPurchaseBalance() { return purchaseAmount.subtract(purchasePaid); }
    public BigDecimal getShippingBalance() { return shippingAmount.subtract(shippingPaid); }

    public void refreshStatus() {
        total = purchaseAmount.add(shippingAmount);
        boolean pp = purchaseAmount.signum() > 0 && purchasePaid.compareTo(purchaseAmount) >= 0;
        boolean sp = shippingAmount.signum() > 0 && shippingPaid.compareTo(shippingAmount) >= 0;
        BigDecimal paid = getPaid();
        if (paid.signum() == 0) status = PayStatus.UNPAID;
        else if (paid.compareTo(total) >= 0) status = PayStatus.PAID;
        else if (pp) status = PayStatus.PURCHASE_PAID;
        else if (sp) status = PayStatus.SHIPPING_PAID;
        else status = PayStatus.PARTIAL;
    }
}
