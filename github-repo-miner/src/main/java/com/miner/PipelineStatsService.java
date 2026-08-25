package com.miner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PipelineStatsService {

    public void save(int totalAvailable, int phase1, int phase2, int phase3, int scored) throws SQLException {
        String sql = """
            INSERT INTO pipeline_stats
            (total_available, phase1_approved, phase2_approved, phase3_approved, scored_count)
            VALUES (?,?,?,?,?)
        """;

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, totalAvailable);
            ps.setInt(2, phase1);
            ps.setInt(3, phase2);
            ps.setInt(4, phase3);
            ps.setInt(5, scored);
            ps.executeUpdate();
        }
        System.out.println("✓ Stats del pipeline guardadas en Postgres");
    }
}