package com.silvanaya.repository;

import com.silvanaya.model.*;
import com.silvanaya.model.Enums.*;
import org.springframework.data.jpa.repository.*;
import java.math.BigDecimal;
import java.util.*;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByCustomerIdOrderByIdDesc(Long customerId);
    long countByStatus(ShipmentStatus status);
}
