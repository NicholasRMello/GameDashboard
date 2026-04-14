package com.nicholas.gamedashboard.api.dto;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateGoalRequest(
    @NotBlank @Size(max = 120) String title,
    @NotNull @DecimalMin("0.0") @DecimalMax("2000.0") Double estimatedHours,
    @NotNull @DecimalMin("0.1") @DecimalMax("24.0") Double hoursPerDay,
    @NotNull @DecimalMin("1") @DecimalMax("7") Integer daysPerWeek,
    @Size(max = 500) String imageUrl,
    Long rawgId,
    String released,
    @DecimalMin("0.0") @DecimalMax("5.0") Double rating,
    List<@Size(max = 40) String> genres,
    @Size(max = 5000) String description
) {
}
