package com.nicholas.dao;

import com.nicholas.model.GameGoal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GameGoalDao {
    private final Connection conn;

    public GameGoalDao() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:goals.db");
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
      CREATE TABLE IF NOT EXISTS goals (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            title           TEXT,
            estimated_hours REAL,
            hours_per_day   REAL,
            days_per_week   INTEGER,
            order_index     INTEGER,
            image_url       TEXT,
            rawg_id         INTEGER,
            released        TEXT,
            rating          REAL,
            genres          TEXT,
            description     TEXT
        )
        """);
        }
    }

    public List<GameGoal> listAll() throws SQLException {
        List<GameGoal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals ORDER BY order_index";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                GameGoal g = new GameGoal();
                g.setId(rs.getLong("id"));
                g.setTitle(rs.getString("title"));
                g.setEstimatedHours(rs.getDouble("estimated_hours"));
                g.setHoursPerDay(rs.getDouble("hours_per_day"));
                g.setDaysPerWeek(rs.getInt("days_per_week"));
                g.setOrderIndex(rs.getInt("order_index"));
                g.setImageUrl(rs.getString("image_url"));
                g.setRawgId(Long.valueOf((Integer)rs.getObject("rawg_id")));              // pode vir null
                String rel = rs.getString("released");
                if (rel != null) g.setReleased(LocalDate.parse(rel));
                g.setRating(rs.getDouble("rating"));
                String genresCsv = rs.getString("genres");
                if (genresCsv != null && !genresCsv.isBlank()) {
                    List<String> genresList = Arrays.stream(genresCsv.split("\\s*,\\s*"))
                            .collect(Collectors.toList());
                    g.setGenres(genresList);
                }
                g.setDescription(rs.getString("description"));
                list.add(g);
            }
        }
        return list;
    }

    public void save(GameGoal g) throws SQLException {
        if (g.getId() == 0) {
            // INSERT with 6 columns + generated_keys
            String sql = """
            INSERT INTO goals(
              title, estimated_hours, hours_per_day,
              days_per_week, order_index, image_url,
              rawg_id, released, rating, genres, description
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
            try (var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, g.getTitle());
                ps.setDouble(2, g.getEstimatedHours());
                ps.setDouble(3, g.getHoursPerDay());
                ps.setInt(4, g.getDaysPerWeek());
                ps.setInt(5, g.getOrderIndex());
                ps.setString(6, g.getImageUrl());
                if (g.getRawgId() != null) ps.setInt(7, g.getRawgId().intValue()); else ps.setNull(7, Types.INTEGER);
                ps.setString(8, g.getReleased() != null ? g.getReleased().toString() : null);
                if (g.getRating() != null) {
                    ps.setDouble(9, g.getRating());
                } else {
                    ps.setNull(9, Types.DOUBLE);
                }
                String genresCsv = g.getGenres() == null ? "" : String.join(", ", g.getGenres());
                ps.setString(10, genresCsv);
                ps.setString(11, g.getDescription());
                ps.executeUpdate();
                try (var rs = ps.getGeneratedKeys()) {
                    if (rs.next()) g.setId(rs.getLong(1));
                }
            }
        } else {
            // UPDATE including image_url and WHERE id=?
            String sql = """
            UPDATE goals SET
              title=?, estimated_hours=?, hours_per_day=?,
              days_per_week=?, order_index=?, image_url=?,
              rawg_id=?, released=?, rating=?, genres=?, description=?
            WHERE id=?
        """;
            try (var ps = conn.prepareStatement(sql)) {
                ps.setString(1, g.getTitle());
                ps.setDouble(2, g.getEstimatedHours());
                ps.setDouble(3, g.getHoursPerDay());
                ps.setInt(4, g.getDaysPerWeek());
                ps.setInt(5, g.getOrderIndex());
                ps.setString(6, g.getImageUrl());
                if (g.getRawgId() != null) ps.setInt(7, g.getRawgId().intValue()); else ps.setNull(7, Types.INTEGER);
                ps.setString(8, g.getReleased() != null ? g.getReleased().toString() : null);
                ps.setDouble(9, g.getRating() != null ? g.getRating() : 0.0);
                ps.setString(10, g.getGenres());
                ps.setString(11, g.getDescription());
                ps.setLong(12, g.getId());
                ps.executeUpdate();
            }
        }
    }


    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM goals WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
