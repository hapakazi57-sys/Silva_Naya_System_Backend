package com.silvanaya.dto;

import com.silvanaya.model.Enums.*;
import com.silvanaya.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    // ---- Auth & Users ----
    public record LoginReq(@NotBlank String email, @NotBlank String password) {}
    public record LoginRes(String token, User user) {}
    public record ProfileReq(@NotBlank String fullName, String phone) {}
    public record PasswordReq(@NotBlank String oldPassword, @NotBlank @Size(min = 8) String newPassword) {}
    public record UserReq(@NotBlank String fullName, @NotBlank @Email String email, String phone,
                          @NotNull Role role, String password, Boolean active) {}
    public record ResetReq(@NotBlank @Size(min = 8) String password) {}

    // ---- Customers ----
    public record CustomerReq(@NotBlank String name, @NotBlank String phone, String email, String address) {}

    // ---- Orders ----
    public record ItemReq(@NotBlank String name, String description,
                          @NotNull @Min(1) Integer quantity, @NotNull @DecimalMin("0") BigDecimal unitPrice) {}
    public record OrderReq(@NotNull Long customerId, @NotBlank String country, @NotBlank String supplierName,
                           @NotBlank String currency, String note, @NotEmpty @Valid List<ItemReq> items) {}
    public record NoteReq(String note) {}
    public record AmountReq(@NotNull @DecimalMin("0") BigDecimal amount) {}
    public record ReasonReq(@NotBlank String reason) {}

    // ---- Shipments (goodsType: null = NORMAL) ----
    public record ShipmentReq(@NotNull Long customerId, Long orderId, @NotNull TransportMode mode,
                              GoodsType goodsType,
                              @NotBlank String originCountry, String origin, String destination,
                              String trackingNo, String description, Integer packages,
                              BigDecimal weightKg, BigDecimal lengthCm, BigDecimal widthCm, BigDecimal heightCm,
                              LocalDate departureDate, LocalDate expectedArrival) {}
    public record CalcReq(@NotNull TransportMode mode, GoodsType goodsType,
                          @NotBlank String originCountry, Integer packages,
                          BigDecimal weightKg, BigDecimal lengthCm, BigDecimal widthCm, BigDecimal heightCm) {}
    public record StatusReq(@NotNull ShipmentStatus status, String note) {}
    public record DeliverReq(@NotBlank String receiverName, @NotBlank String receiverPhone, String note) {}

    // ---- Invoices & Payments ----
    public record InvoiceReq(@NotNull Long customerId, Long orderId, Long shipmentId, LocalDate dueDate) {}
    public record AllocReq(@NotNull Long invoiceId, @NotNull PaymentPart part,
                           @NotNull @DecimalMin("0.01") BigDecimal amountTzs) {}
    public record PaymentReq(@NotNull Long customerId, @NotNull @DecimalMin("0.01") BigDecimal amount,
                             @NotBlank String currency, String note,
                             @NotEmpty @Valid List<AllocReq> allocations) {}

    // ---- Rates ----
    public record RateReq(@NotNull TransportMode mode, GoodsType goodsType, @NotBlank String originCountry,
                          BigDecimal ratePerKg, BigDecimal ratePerCbm, BigDecimal minCharge,
                          Integer volumetricDivisor, @NotBlank String currency, LocalDate effectiveFrom) {}
    public record ExchangeReq(@NotBlank String currency, @NotNull @DecimalMin("0.0001") BigDecimal rateToTzs,
                              LocalDate effectiveFrom) {}
}