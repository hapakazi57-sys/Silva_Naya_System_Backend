package com.silvanaya.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.silvanaya.model.Enums.Role;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String fullName;
    @Column(unique = true, nullable = false) private String email;
    private String phone;
    @JsonIgnore private String password;
    @Enumerated(EnumType.STRING) private Role role;
    private boolean active = true;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt = LocalDateTime.now();
}
