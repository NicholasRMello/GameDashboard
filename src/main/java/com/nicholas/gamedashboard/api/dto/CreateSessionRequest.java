package com.nicholas.gamedashboard.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateSessionRequest(
    @NotNull LocalDate playDate,
    @NotNull @DecimalMin("0.1") @DecimalMax("24.0") Double hoursPlayed
) {
}
