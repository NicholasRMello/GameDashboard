package com.nicholas.gamedashboard.api.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SessionResponse(
    Long id,
    Long goalId,
    LocalDate playDate,
    double hoursPlayed,
    Instant createdAt
) {
}
