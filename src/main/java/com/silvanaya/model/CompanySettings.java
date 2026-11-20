package com.silvanaya.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Mipangilio ya kampuni: safu moja tu (id = 1). */
@Entity @Table(name = "company_settings") @Getter @Setter @NoArgsConstructor
public class CompanySettings {
    @Id private Long id = 1L;

    /* ---------- Kampuni ---------- */
    private String companyName = "SILVA NAYA COMPANY LTD";
    @Column(columnDefinition = "text") private String logoData;   // data URL (picha)
    @Column(length = 300) private String address;
    private String phone;
    private String email;

    /* ---------- Sarafu na saa ---------- */
    private String baseCurrency = "TZS";
    @Column(length = 300) private String currencies = "TZS,USD,CNY,AED";
    private String timezone = "Africa/Dar_es_Salaam";

    /* ---------- Muundo wa ankara na risiti ---------- */
    private String accentColor = "#0b5fff";
    private String invoiceTitle = "INVOICE";
    private boolean invoiceShowLogo = true;
    @Column(length = 1000) private String invoiceTerms = "Payment is due by the due date shown on this invoice.";
    @Column(length = 500) private String invoiceFooter = "Thank you for your business.";
    private String receiptTitle = "PAYMENT RECEIPT";
    private boolean receiptShowLogo = true;
    @Column(length = 500) private String receiptFooter = "This receipt confirms the payment received. Thank you.";

    /* ---------- Namba za nyaraka ---------- */
    private String invoicePrefix = "INV";
    private String receiptPrefix = "RCT";
    private String transactionPrefix = "TRX";
    private boolean numberIncludeYear = true;
    private String numberSeparator = "-";
    private int numberPadding = 5;

    private LocalDateTime updatedAt;
    private String updatedBy;
}


