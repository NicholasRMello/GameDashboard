package com.nicholas.dao;

import com.nicholas.dao.GameGoalDao;
import com.nicholas.model.GameGoal;

import java.sql.SQLException;
import java.util.List;

public class DaoTest {
    public static void main(String[] args) throws SQLException {
        GameGoalDao dao = new GameGoalDao();

        // 1) Create a new game
        GameGoal novo = new GameGoal();
        novo.setTitle("Test Game");
        novo.setEstimatedHours(10);
        novo.setHoursPerDay(1.5);
        novo.setDaysPerWeek(5);
        novo.setOrderIndex(0);
        dao.save(novo);
        System.out.println("Inserted ID = " + novo.getId());

        // 2) List All
        List<GameGoal> all = dao.listAll();
        all.forEach(g -> System.out.println("→ " + g.getId() + " : " + g.getTitle()));

        // 3) Update
        novo.setEstimatedHours(12);
        dao.save(novo);
        System.out.println("After Update: ");
        dao.listAll().forEach(g ->
                System.out.println("→ " + g.getTitle() + " → " + g.getEstimatedHours()));

        // 4) Delete
        dao.delete(novo.getId());
        System.out.println("After Delete, count = " + dao.listAll().size());
    }
}
