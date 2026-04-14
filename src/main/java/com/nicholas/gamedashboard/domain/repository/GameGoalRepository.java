package com.nicholas.gamedashboard.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nicholas.gamedashboard.domain.entity.GameGoalEntity;

public interface GameGoalRepository extends JpaRepository<GameGoalEntity, Long> {
    List<GameGoalEntity> findAllByOrderByDisplayOrderAsc();
}
