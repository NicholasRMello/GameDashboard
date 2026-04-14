package com.nicholas.gamedashboard.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nicholas.gamedashboard.domain.entity.PlaySessionEntity;

public interface PlaySessionRepository extends JpaRepository<PlaySessionEntity, Long> {

    List<PlaySessionEntity> findByGoalIdOrderByPlayDateAsc(Long goalId);

    @Query("select coalesce(sum(s.hoursPlayed), 0) from PlaySessionEntity s where s.goal.id = :goalId")
    double sumHoursByGoalId(@Param("goalId") Long goalId);

    @Query("select coalesce(sum(s.hoursPlayed), 0) from PlaySessionEntity s")
    double sumTotalHours();
}
