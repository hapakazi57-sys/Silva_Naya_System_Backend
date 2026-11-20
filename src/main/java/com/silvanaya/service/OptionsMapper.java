package com.silvanaya.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component @RequiredArgsConstructor
public class OptionsMapper {
    private final ObjectMapper mapper;

    @SuppressWarnings("unchecked")
    public Map<String, Object> pick(Object entity, String... keys) {
        Map<String, Object> src = mapper.convertValue(entity, Map.class);
        Map<String, Object> out = new LinkedHashMap<>();
        for (String k : keys) if (src.containsKey(k)) out.put(k, src.get(k));
        Object c = src.get("customer");
        if (c instanceof Map<?, ?> cm) {
            Map<String, Object> slim = new LinkedHashMap<>();
            slim.put("id", cm.get("id"));
            slim.put("name", cm.get("name"));
            out.put("customer", slim);
        }
        return out;
    }
}