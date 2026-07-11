package com.miner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportGenerator {

    public String generateMarkdownReport() throws SQLException {
        StringBuilder md = new StringBuilder();

        try (Connection conn = DatabaseManager.connect()) {
            md.append("# Reporte de Scoring y Ranking — Framework de Minería GitHub\n\n");
            md.append("Generado: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
              .append("\n\n");

            appendSummaryStats(conn, md);
            appendTopRanking(conn, md);
            appendFrameworkBreakdown(conn, md);
            appendTechnicalReadiness(conn, md);
        }

        return md.toString();
    }

    private void appendSummaryStats(Connection conn, StringBuilder md) throws SQLException {
        md.append("## Resumen General\n\n");

        String sql = """
            SELECT COUNT(*) as total,
                   ROUND(AVG(total_score), 1) as avg_score,
                   ROUND(MAX(total_score), 1) as max_score,
                   ROUND(MIN(total_score), 1) as min_score
            FROM repo_scores
        """;

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                md.append("| Métrica | Valor |\n|---|---|\n");
                md.append("| Total de repositorios evaluados | ").append(rs.getInt("total")).append(" |\n");
                md.append("| Score promedio | ").append(rs.getDouble("avg_score")).append("/100 |\n");
                md.append("| Score máximo | ").append(rs.getDouble("max_score")).append("/100 |\n");
                md.append("| Score mínimo | ").append(rs.getDouble("min_score")).append("/100 |\n\n");
            }
        }
    }

    private void appendTopRanking(Connection conn, StringBuilder md) throws SQLException {
        md.append("## Top 15 del Ranking\n\n");
        md.append("| Rank | Repositorio | Total | Técnico | Tests | CI/Higiene | Sector | Popularidad | Mantenim. |\n");
        md.append("|---|---|---|---|---|---|---|---|---|\n");

        String sql = """
            SELECT rank, full_name, total_score, technical_score, test_quality_score,
                   ci_hygiene_score, sector_score, popularity_score, maintenance_score
            FROM repo_scores
            ORDER BY rank ASC
            LIMIT 15
        """;

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                md.append("| ").append(rs.getInt("rank"))
                  .append(" | ").append(rs.getString("full_name"))
                  .append(" | ").append(String.format("%.1f", rs.getDouble("total_score")))
                  .append(" | ").append(String.format("%.1f", rs.getDouble("technical_score")))
                  .append(" | ").append(String.format("%.1f", rs.getDouble("test_quality_score")))
                  .append(" | ").append(String.format("%.1f", rs.getDouble("ci_hygiene_score")))
                  .append(" | ").append(String.format("%.1f", rs.getDouble("sector_score")))
                  .append(" | ").append(String.format("%.1f", rs.getDouble("popularity_score")))
                  .append(" | ").append(String.format("%.1f", rs.getDouble("maintenance_score")))
                  .append(" |\n");
            }
        }
        md.append("\n");
    }

    private void appendFrameworkBreakdown(Connection conn, StringBuilder md) throws SQLException {
        md.append("## Distribución por Framework\n\n");
        md.append("| Framework | Cantidad | Score Promedio |\n|---|---|---|\n");

        String sql = """
            SELECT t.framework, COUNT(*) as cantidad, ROUND(AVG(s.total_score), 1) as avg_score
            FROM tech_profiles t
            JOIN repo_scores s ON t.full_name = s.full_name
            GROUP BY t.framework
            ORDER BY cantidad DESC
        """;

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                md.append("| ").append(rs.getString("framework"))
                  .append(" | ").append(rs.getInt("cantidad"))
                  .append(" | ").append(rs.getDouble("avg_score")).append("/100 |\n");
            }
        }
        md.append("\n");
    }

    private void appendTechnicalReadiness(Connection conn, StringBuilder md) throws SQLException {
        md.append("## Preparación Técnica (candidatos a benchmarking)\n\n");

        String sql = """
            SELECT
                SUM(CASE WHEN graalvm_ready = 1 THEN 1 ELSE 0 END) as graalvm_count,
                SUM(CASE WHEN jmh_present = 1 THEN 1 ELSE 0 END) as jmh_confirmed,
                SUM(CASE WHEN jmh_candidate = 1 AND jmh_present = 0 THEN 1 ELSE 0 END) as jmh_heuristic,
                SUM(CASE WHEN travis_ci = 1 THEN 1 ELSE 0 END) as travis_count,
                COUNT(*) as total
            FROM tech_profiles
        """;

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int total = rs.getInt("total");
                md.append("| Criterio | Cantidad | % del total |\n|---|---|---|\n");
                md.append("| GraalVM ready | ").append(rs.getInt("graalvm_count"))
                  .append(" | ").append(pct(rs.getInt("graalvm_count"), total)).append(" |\n");
                md.append("| JMH confirmado | ").append(rs.getInt("jmh_confirmed"))
                  .append(" | ").append(pct(rs.getInt("jmh_confirmed"), total)).append(" |\n");
                md.append("| JMH candidato (heurístico) | ").append(rs.getInt("jmh_heuristic"))
                  .append(" | ").append(pct(rs.getInt("jmh_heuristic"), total)).append(" |\n");
                md.append("| Travis CI | ").append(rs.getInt("travis_count"))
                  .append(" | ").append(pct(rs.getInt("travis_count"), total)).append(" |\n\n");
            }
        }
    }

    private String pct(int count, int total) {
        if (total == 0) return "0%";
        return String.format("%.1f%%", (count * 100.0) / total);
    }
}