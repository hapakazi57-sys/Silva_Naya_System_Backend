package com.silvanaya.controller;

import com.silvanaya.model.User;
import com.silvanaya.model.Enums.*;
import com.silvanaya.repository.*;
import com.silvanaya.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController @RequiredArgsConstructor
public class DashboardController {
    private final CustomerRepository customers;
    private final PurchaseOrderRepository orders;
    private final ShipmentRepository shipments;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final ActivityLogRepository logs;

    @GetMapping("/api/dashboard")
    public Map<String, Object> dashboard() {
        User me = CurrentUser.require();
        boolean admin = me.getRole() == Role.ADMIN, employee = me.getRole() == Role.EMPLOYEE;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customers", customers.count());
        m.put("orders", orders.count());
        m.put("shipments", shipments.count());
        m.put("inTransit", shipments.countByStatus(ShipmentStatus.IN_TRANSIT));
        m.put("arrivedNotCollected", shipments.countByStatus(ShipmentStatus.ARRIVED) + shipments.countByStatus(ShipmentStatus.READY_FOR_PICKUP));
        m.put("delivered", shipments.countByStatus(ShipmentStatus.DELIVERED));
        if (!admin) {
            m.put("invoicesUnpaid", invoices.countByStatusAndVoidedFalse(PayStatus.UNPAID));
            m.put("invoicesPartial", invoices.countByStatusAndVoidedFalse(PayStatus.PARTIAL)
                    + invoices.countByStatusAndVoidedFalse(PayStatus.PURCHASE_PAID)
                    + invoices.countByStatusAndVoidedFalse(PayStatus.SHIPPING_PAID));
            m.put("totalDebt", invoices.totalDebt());
            m.put("totalReceived", payments.totalReceived());
            m.put("ordersPending", orders.countByStatus(OrderStatus.SUBMITTED));
        }
        if (admin) {
            m.put("usersTotal", users.count());
            long active = users.findAll().stream().filter(User::isActive).count();
            m.put("usersActive", active);
            m.put("usersInactive", users.count() - active);
        }
        m.put("recent", employee ? logs.findTop10ByUserIdOrderByIdDesc(me.getId()) : logs.findTop10ByOrderByIdDesc());
        return m;
    }

    @GetMapping("/api/logs")
    public Object logs() {
        User me = CurrentUser.require();
        return me.getRole() == Role.EMPLOYEE ? logs.findTop200ByUserIdOrderByIdDesc(me.getId()) : logs.findTop200ByOrderByIdDesc();
    }
}
