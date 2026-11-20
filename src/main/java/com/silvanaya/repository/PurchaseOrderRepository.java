package com.silvanaya.repository;

import com.silvanaya.model.*;
import com.silvanaya.model.Enums.*;
import org.springframework.data.jpa.repository.*;
import java.math.BigDecimal;
import java.util.*;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByCustomerIdOrderByIdDesc(Long customerId);
    long countByStatus(OrderStatus status);
}
