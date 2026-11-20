package com.silvanaya.repository;

import com.silvanaya.model.*;
import com.silvanaya.model.Enums.*;
import org.springframework.data.jpa.repository.*;
import java.math.BigDecimal;
import java.util.*;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByCustomerIdOrderByIdDesc(Long customerId);
    boolean existsByShipmentIdAndVoidedFalse(Long shipmentId);
    boolean existsByPurchaseOrderIdAndVoidedFalse(Long orderId);
    long countByStatusAndVoidedFalse(PayStatus status);
    @Query("select coalesce(sum(i.total - i.purchasePaid - i.shippingPaid),0) from Invoice i where i.voided=false")
    BigDecimal totalDebt();
}
