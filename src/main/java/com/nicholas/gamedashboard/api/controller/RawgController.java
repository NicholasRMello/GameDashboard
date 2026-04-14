package com.nicholas.gamedashboard.api.controller;

import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nicholas.gamedashboard.api.dto.RawgLookupResponse;
import com.nicholas.gamedashboard.service.RawgService;

@RestController
@RequestMapping("/api/v1/rawg")
public class RawgController {

    private final RawgService rawgService;

    public RawgController(RawgService rawgService) {
        this.rawgService = rawgService;
    }

    @GetMapping("/lookup")
    public Optional<RawgLookupResponse> lookup(@RequestParam String title) {
        return rawgService.lookup(title);
    }
}
