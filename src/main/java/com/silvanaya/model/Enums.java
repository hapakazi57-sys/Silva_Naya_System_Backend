package com.silvanaya.model;

public final class Enums {
    private Enums() {}
    public enum Role { EMPLOYEE, MANAGER, ADMIN }
    public enum TransportMode { AIR, SEA }
    public enum GoodsType { NORMAL, HK }
    public enum OrderStatus { DRAFT, SUBMITTED, APPROVED, REJECTED, REVISION }
    public enum ShipmentStatus { RECEIVED_ORIGIN, IN_TRANSIT, ARRIVED, READY_FOR_PICKUP, DELIVERED, ON_HOLD }
    public enum InvoiceType { PURCHASE, SHIPPING, COMBINED }
    public enum PaymentPart { PURCHASE, SHIPPING }
    public enum PayStatus { UNPAID, PARTIAL, PURCHASE_PAID, SHIPPING_PAID, PAID }
}
