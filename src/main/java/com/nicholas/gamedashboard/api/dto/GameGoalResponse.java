package com.nicholas.gamedashboard.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record GameGoalResponse(
    Long id,
    String title,
    double estimatedHours,
    double hoursPerDay,
    int daysPerWeek,
    int displayOrder,
    String imageUrl,
    Long rawgId,
    LocalDate released,
    Double rating,
    List<String> genres,
    String description,
    double playedHours,
    double remainingHours,
    double remainingDays,
    double progressPercent,
    Instant createdAt,
    Instant updatedAt
) {
}
