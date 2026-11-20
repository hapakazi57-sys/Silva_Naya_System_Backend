package com.silvanaya.model;

import com.silvanaya.model.Enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "shipments") @Getter @Setter @NoArgsConstructor
public class Shipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true) private String cargoNo;
    private String trackingNo;
    @ManyToOne private Customer customer;
    @ManyToOne private PurchaseOrder purchaseOrder;
    @Enumerated(EnumType.STRING) private TransportMode mode;
    @Enumerated(EnumType.STRING) private GoodsType goodsType = GoodsType.NORMAL;   // NORMAL au HK
    private String originCountry;
    private String origin;
    private String destination;
    private String description;
    private int packages = 1;
    private BigDecimal weightKg = BigDecimal.ZERO;
    private BigDecimal lengthCm = BigDecimal.ZERO;
    private BigDecimal widthCm = BigDecimal.ZERO;
    private BigDecimal heightCm = BigDecimal.ZERO;
    private BigDecimal cbm = BigDecimal.ZERO;
    private BigDecimal chargeableKg = BigDecimal.ZERO;
    // Bei inahifadhiwa mzigo unaposajiliwa (bei mpya hazibadilishi za zamani)
    private BigDecimal chargeAmount = BigDecimal.ZERO;
    private String chargeCurrency = "USD";
    private BigDecimal chargeTzs = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) private ShipmentStatus status = ShipmentStatus.RECEIVED_ORIGIN;
    private LocalDate departureDate;
    private LocalDate expectedArrival;
    private LocalDate actualArrival;
    private String receiverName;
    private String receiverPhone;
    private LocalDateTime deliveredAt;
    private String deliveryNote;
    @ManyToOne private User createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();
}