package com.nicholas.gamedashboard.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nicholas.gamedashboard.api.dto.CreateGoalRequest;
import com.nicholas.gamedashboard.api.dto.CreateSessionRequest;
import com.nicholas.gamedashboard.api.dto.GameGoalResponse;
import com.nicholas.gamedashboard.api.dto.ReorderGoalsRequest;
import com.nicholas.gamedashboard.api.dto.SessionResponse;
import com.nicholas.gamedashboard.api.dto.UpdateGoalRequest;
import com.nicholas.gamedashboard.service.GameDashboardService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/goals")
@Validated
public class GameGoalController {

    private final GameDashboardService gameDashboardService;

    public GameGoalController(GameDashboardService gameDashboardService) {
        this.gameDashboardService = gameDashboardService;
    }

    @GetMapping
    public List<GameGoalResponse> listGoals() {
        return gameDashboardService.listGoals();
    }

    @GetMapping("/{goalId}")
    public GameGoalResponse getGoal(@PathVariable Long goalId) {
        return gameDashboardService.getGoal(goalId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameGoalResponse createGoal(@Valid @RequestBody CreateGoalRequest request) {
        return gameDashboardService.createGoal(request);
    }

    @PutMapping("/{goalId}")
    public GameGoalResponse updateGoal(@PathVariable Long goalId, @Valid @RequestBody UpdateGoalRequest request) {
        return gameDashboardService.updateGoal(goalId, request);
    }

    @DeleteMapping("/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGoal(@PathVariable Long goalId) {
        gameDashboardService.deleteGoal(goalId);
    }

    @PostMapping("/reorder")
    public List<GameGoalResponse> reorderGoals(@Valid @RequestBody ReorderGoalsRequest request) {
        return gameDashboardService.reorderGoals(request);
    }

    @GetMapping("/{goalId}/sessions")
    public List<SessionResponse> listSessions(@PathVariable Long goalId) {
        return gameDashboardService.listSessions(goalId);
    }

    @PostMapping("/{goalId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse createSession(@PathVariable Long goalId, @Valid @RequestBody CreateSessionRequest request) {
        return gameDashboardService.createSession(goalId, request);
    }

    @DeleteMapping("/{goalId}/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(@PathVariable Long goalId, @PathVariable Long sessionId) {
        gameDashboardService.deleteSession(goalId, sessionId);
    }
}
