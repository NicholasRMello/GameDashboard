package com.nicholas.dao;

import com.nicholas.model.Session;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SessionDao {
    private final Connection conn;

    public SessionDao() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:goals.db");
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
              CREATE TABLE IF NOT EXISTS sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                game_id INTEGER,
                play_date TEXT,
                hours_played REAL,
                FOREIGN KEY(game_id) REFERENCES goals(id)
              )""");
        }
    }

    public List<Session> listByGame(long gameId) throws SQLException {
        List<Session> list = new ArrayList<>();
        String sql = "SELECT * FROM sessions WHERE game_id=? ORDER BY play_date";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Session s = new Session();
                    s.setId(rs.getLong("id"));
                    s.setGameId(rs.getLong("game_id"));
                    s.setPlayDate(LocalDate.parse(rs.getString("play_date")));
                    s.setHoursPlayed(rs.getDouble("hours_played"));
                    list.add(s);
                }
            }
        }
        return list;
    }

    public void save(Session s) throws SQLException {
        if (s.getId() == 0) {
            String ins = "INSERT INTO sessions(game_id,play_date,hours_played) VALUES(?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, s.getGameId());
                ps.setString(2, s.getPlayDate().toString());
                ps.setDouble(3, s.getHoursPlayed());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) s.setId(keys.getLong(1));
                }
            }
        } else {
            String upd = "UPDATE sessions SET play_date=?,hours_played=? WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(upd)) {
                ps.setString(1, s.getPlayDate().toString());
                ps.setDouble(2, s.getHoursPlayed());
                ps.setLong(3, s.getId());
                ps.executeUpdate();
            }
        }
    }

    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sessions WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
