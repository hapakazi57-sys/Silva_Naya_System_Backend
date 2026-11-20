package com.silvanaya.controller;

import com.silvanaya.model.CompanySettings;
import com.silvanaya.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/settings") @RequiredArgsConstructor
public class SettingsController {
    private final SettingsService service;

    /** Mtumiaji yeyote aliyeingia anaweza kusoma (jina la kampuni, nembo, sarafu). */
    @GetMapping
    public CompanySettings get() { return service.get(); }

    /** Msimamizi pekee (inakaguliwa ndani ya service). */
    @PutMapping
    public CompanySettings update(@RequestBody CompanySettings body) { return service.update(body); }
}