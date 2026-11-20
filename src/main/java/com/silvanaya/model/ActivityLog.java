package com.silvanaya.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "activity_logs") @Getter @Setter @NoArgsConstructor
public class ActivityLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long userId;
    private String userName;
    private String action;
    private String entity;
    @Column(length = 600) private String details;
    private LocalDateTime createdAt = LocalDateTime.now();
}
