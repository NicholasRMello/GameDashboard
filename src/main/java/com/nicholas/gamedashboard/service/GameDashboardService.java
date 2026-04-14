package com.nicholas.gamedashboard.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nicholas.gamedashboard.api.NotFoundException;
import com.nicholas.gamedashboard.api.dto.CreateGoalRequest;
import com.nicholas.gamedashboard.api.dto.CreateSessionRequest;
import com.nicholas.gamedashboard.api.dto.DashboardSummaryResponse;
import com.nicholas.gamedashboard.api.dto.GameGoalResponse;
import com.nicholas.gamedashboard.api.dto.ReorderGoalsRequest;
import com.nicholas.gamedashboard.api.dto.SessionResponse;
import com.nicholas.gamedashboard.api.dto.UpdateGoalRequest;
import com.nicholas.gamedashboard.domain.entity.GameGoalEntity;
import com.nicholas.gamedashboard.domain.entity.PlaySessionEntity;
import com.nicholas.gamedashboard.domain.repository.GameGoalRepository;
import com.nicholas.gamedashboard.domain.repository.PlaySessionRepository;

@Service
public class GameDashboardService {

    private final GameGoalRepository goalRepository;
    private final PlaySessionRepository sessionRepository;

    public GameDashboardService(GameGoalRepository goalRepository, PlaySessionRepository sessionRepository) {
        this.goalRepository = goalRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public List<GameGoalResponse> listGoals() {
        return goalRepository.findAllByOrderByDisplayOrderAsc().stream()
            .map(this::toGoalResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public GameGoalResponse getGoal(Long goalId) {
        GameGoalEntity goal = goalRepository.findById(goalId)
            .orElseThrow(() -> new NotFoundException("Goal not found"));
        return toGoalResponse(goal);
    }

    @Transactional
    public GameGoalResponse createGoal(CreateGoalRequest request) {
        int nextOrder = goalRepository.findAll().size();
        GameGoalEntity goal = new GameGoalEntity();
        applyGoalRequest(goal, request.title(), request.estimatedHours(), request.hoursPerDay(), request.daysPerWeek(), request.price(), request.currency(),
            request.imageUrl(), request.rawgId(), request.released(), request.rating(), request.genres(), request.description());
        goal.setDisplayOrder(nextOrder);

        GameGoalEntity saved = goalRepository.save(goal);
        return toGoalResponse(saved);
    }

    @Transactional
    public GameGoalResponse updateGoal(Long goalId, UpdateGoalRequest request) {
        GameGoalEntity goal = goalRepository.findById(goalId)
            .orElseThrow(() -> new NotFoundException("Goal not found"));

        applyGoalRequest(goal, request.title(), request.estimatedHours(), request.hoursPerDay(), request.daysPerWeek(), request.price(), request.currency(),
            request.imageUrl(), request.rawgId(), request.released(), request.rating(), request.genres(), request.description());

        return toGoalResponse(goalRepository.save(goal));
    }

    @Transactional
    public void deleteGoal(Long goalId) {
        GameGoalEntity goal = goalRepository.findById(goalId)
            .orElseThrow(() -> new NotFoundException("Goal not found"));

        List<PlaySessionEntity> sessions = sessionRepository.findByGoalIdOrderByPlayDateAsc(goalId);
        sessionRepository.deleteAll(sessions);
        goalRepository.delete(goal);

        List<GameGoalEntity> remaining = goalRepository.findAllByOrderByDisplayOrderAsc();
        for (int index = 0; index < remaining.size(); index++) {
            remaining.get(index).setDisplayOrder(index);
        }
    }

    @Transactional
    public List<GameGoalResponse> reorderGoals(ReorderGoalsRequest request) {
        List<GameGoalEntity> all = goalRepository.findAllByOrderByDisplayOrderAsc();
        if (request.orderedGoalIds().size() != all.size()) {
            throw new IllegalArgumentException("Reorder payload must include all goal IDs");
        }

        for (int index = 0; index < request.orderedGoalIds().size(); index++) {
            Long goalId = request.orderedGoalIds().get(index);
            GameGoalEntity goal = all.stream()
                .filter(item -> item.getId().equals(goalId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Goal not found in reorder list: " + goalId));
            goal.setDisplayOrder(index);
        }

        goalRepository.saveAll(all);
        return listGoals();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(Long goalId) {
        if (!goalRepository.existsById(goalId)) {
            throw new NotFoundException("Goal not found");
        }

        return sessionRepository.findByGoalIdOrderByPlayDateAsc(goalId).stream()
            .map(this::toSessionResponse)
            .toList();
    }

    @Transactional
    public SessionResponse createSession(Long goalId, CreateSessionRequest request) {
        GameGoalEntity goal = goalRepository.findById(goalId)
            .orElseThrow(() -> new NotFoundException("Goal not found"));

        PlaySessionEntity session = new PlaySessionEntity();
        session.setGoal(goal);
        session.setPlayDate(request.playDate());
        session.setHoursPlayed(request.hoursPlayed());

        return toSessionResponse(sessionRepository.save(session));
    }

    @Transactional
    public void deleteSession(Long goalId, Long sessionId) {
        if (!goalRepository.existsById(goalId)) {
            throw new NotFoundException("Goal not found");
        }

        PlaySessionEntity session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Session not found"));

        if (!session.getGoal().getId().equals(goalId)) {
            throw new NotFoundException("Session does not belong to this goal");
        }

        sessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        List<GameGoalEntity> goals = goalRepository.findAll();
        double totalEstimated = goals.stream().mapToDouble(GameGoalEntity::getEstimatedHours).sum();
        double totalPlayed = sessionRepository.sumTotalHours();
        double totalRemainingHours = Math.max(0, totalEstimated - totalPlayed);

        double totalRemainingDays = goals.stream()
            .mapToDouble(goal -> {
                double played = sessionRepository.sumHoursByGoalId(goal.getId());
                double remainingHours = Math.max(0, goal.getEstimatedHours() - played);
                return remainingHours / Math.max(goal.getHoursPerDay(), 0.1);
            })
            .sum();

        return new DashboardSummaryResponse(
            goals.size(),
            round(totalEstimated),
            round(totalPlayed),
            round(totalRemainingHours),
            round(totalRemainingDays)
        );
    }

    private void applyGoalRequest(
        GameGoalEntity goal,
        String title,
        Double estimatedHours,
        Double hoursPerDay,
        Integer daysPerWeek,
        Double price,
        String currency,
        String imageUrl,
        Long rawgId,
        String released,
        Double rating,
        List<String> genres,
        String description
    ) {
        goal.setTitle(title.trim());
        goal.setEstimatedHours(estimatedHours);
        goal.setHoursPerDay(hoursPerDay);
        goal.setDaysPerWeek(daysPerWeek);
        goal.setPrice(price);
        goal.setCurrency(currency == null ? "BRL" : currency.trim().toUpperCase());
        goal.setImageUrl(blankToNull(imageUrl));
        goal.setRawgId(rawgId);
        goal.setReleased(parseDateSafely(released));
        goal.setRating(rating);
        goal.setGenresCsv(toGenresCsv(genres));
        goal.setDescription(blankToNull(description));
    }

    private GameGoalResponse toGoalResponse(GameGoalEntity goal) {
        double playedHours = sessionRepository.sumHoursByGoalId(goal.getId());
        double remainingHours = Math.max(0, goal.getEstimatedHours() - playedHours);
        double remainingDays = remainingHours / Math.max(goal.getHoursPerDay(), 0.1);
        double progressPercent = goal.getEstimatedHours() <= 0 ? 0 : (playedHours / goal.getEstimatedHours()) * 100;

        return new GameGoalResponse(
            goal.getId(),
            goal.getTitle(),
            round(goal.getEstimatedHours()),
            round(goal.getHoursPerDay()),
            goal.getDaysPerWeek(),
            round(goal.getPrice()),
            goal.getCurrency(),
            goal.getDisplayOrder(),
            goal.getImageUrl(),
            goal.getRawgId(),
            goal.getReleased(),
            goal.getRating(),
            toGenres(goal.getGenresCsv()),
            goal.getDescription(),
            round(playedHours),
            round(remainingHours),
            round(remainingDays),
            round(progressPercent),
            goal.getCreatedAt(),
            goal.getUpdatedAt()
        );
    }

    private SessionResponse toSessionResponse(PlaySessionEntity session) {
        return new SessionResponse(
            session.getId(),
            session.getGoal().getId(),
            session.getPlayDate(),
            round(session.getHoursPlayed()),
            session.getCreatedAt()
        );
    }

    private List<String> toGenres(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
    }

    private String toGenresCsv(List<String> genres) {
        if (genres == null || genres.isEmpty()) {
            return null;
        }

        return genres.stream()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .distinct()
            .collect(Collectors.joining(", "));
    }

    private LocalDate parseDateSafely(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
