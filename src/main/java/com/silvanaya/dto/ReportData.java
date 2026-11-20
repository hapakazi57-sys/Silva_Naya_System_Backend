package com.silvanaya.dto;

import java.util.List;

public record ReportData(
        String type,
        String title,
        String period,
        String from,
        String to,
        List<Col> columns,
        List<List<Object>> rows,
        List<Stat> summary,
        String generatedBy,
        String generatedAt) {

    public record Col(String key, String label, String type, String align) {}

    public record Stat(String label, String value) {}
}