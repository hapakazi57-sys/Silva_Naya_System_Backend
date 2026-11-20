package com.silvanaya.controller;

import com.silvanaya.dto.ReportData;
import com.silvanaya.service.AuditService;
import com.silvanaya.service.ReportExportService;
import com.silvanaya.service.ReportService;
import com.silvanaya.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;



@RestController @RequestMapping("/api/reports") @RequiredArgsConstructor
public class ReportController {
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportService reports;
    private final ReportExportService exporter;
    private final SettingsService settings;
    private final AuditService audit;

    @GetMapping("/{type}")
    public ReportData get(@PathVariable String type,
                          @RequestParam(defaultValue = "monthly") String period,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.build(type, period, date, from, to);
    }

    @GetMapping("/{type}/export")
    public ResponseEntity<byte[]> export(@PathVariable String type,
                                         @RequestParam(defaultValue = "monthly") String period,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(defaultValue = "pdf") String format) throws IOException {
        ReportData d = reports.build(type, period, date, from, to);   // pia inakagua role
        boolean xlsx = "xlsx".equalsIgnoreCase(format);
        byte[] bytes = xlsx ? exporter.excel(d, settings.get()) : exporter.pdf(d, settings.get());
        String name = d.type() + "-report-" + d.from() + "_" + d.to() + (xlsx ? ".xlsx" : ".pdf");
        audit.log("EXPORT", "Report", d.title() + " (" + d.from() + " to " + d.to() + ") as " + (xlsx ? "Excel" : "PDF"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .contentType(xlsx ? MediaType.parseMediaType(XLSX) : MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}