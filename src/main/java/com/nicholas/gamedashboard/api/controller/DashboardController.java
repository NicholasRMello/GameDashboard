package com.nicholas.gamedashboard.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nicholas.gamedashboard.api.dto.DashboardSummaryResponse;
import com.nicholas.gamedashboard.service.GameDashboardService;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final GameDashboardService gameDashboardService;

    public DashboardController(GameDashboardService gameDashboardService) {
        this.gameDashboardService = gameDashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return gameDashboardService.getSummary();
    }
}
