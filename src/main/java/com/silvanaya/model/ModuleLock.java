package com.silvanaya.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Password protecting one menu module (orders, shipments, invoices, payments). One row per module. */
@Entity @Table(name = "module_lock") @Getter @Setter @NoArgsConstructor
public class ModuleLock {
    @Id private String module;
    private String passwordHash;
    private String updatedBy;
    private LocalDateTime updatedAt;
}