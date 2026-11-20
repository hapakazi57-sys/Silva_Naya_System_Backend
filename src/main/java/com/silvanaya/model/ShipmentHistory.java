package com.silvanaya.model;

import com.silvanaya.model.Enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "shipment_history") @Getter @Setter @NoArgsConstructor
public class ShipmentHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long shipmentId;
    @Enumerated(EnumType.STRING) private ShipmentStatus status;
    private String note;
    private String changedBy;
    private LocalDateTime changedAt = LocalDateTime.now();
}
