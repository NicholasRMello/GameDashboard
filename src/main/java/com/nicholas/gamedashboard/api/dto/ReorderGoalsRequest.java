package com.nicholas.gamedashboard.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record ReorderGoalsRequest(
    @NotEmpty List<Long> orderedGoalIds
) {
}
