package com.nicholas.gamedashboard.api.dto;

public record DashboardSummaryResponse(
    int totalGames,
    double totalEstimatedHours,
    double totalPlayedHours,
    double totalRemainingHours,
    double totalRemainingDays
) {
}
