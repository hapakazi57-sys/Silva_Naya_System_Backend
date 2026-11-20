package com.silvanaya.repository;

import com.silvanaya.model.*;
import com.silvanaya.model.Enums.*;
import org.springframework.data.jpa.repository.*;
import java.math.BigDecimal;
import java.util.*;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByCustomerIdOrderByIdDesc(Long customerId);
    @Query("select coalesce(sum(p.amountTzs),0) from Payment p where p.receiptVoided=false")
    BigDecimal totalReceived();
}
