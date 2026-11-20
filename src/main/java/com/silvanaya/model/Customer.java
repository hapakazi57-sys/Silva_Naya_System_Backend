package com.silvanaya.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "customers") @Getter @Setter @NoArgsConstructor
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true) private String customerNo;
    @Column(nullable = false) private String name;
    private String phone;
    private String email;
    private String address;
    private LocalDateTime createdAt = LocalDateTime.now();
}
