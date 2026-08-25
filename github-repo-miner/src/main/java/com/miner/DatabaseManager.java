package com.miner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    // Configurable vía variables de entorno — mismos defaults para desarrollo
    // local y para el contenedor de Docker Compose (docker-compose.yml)
    private static final String DB_HOST     = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT     = System.getenv().getOrDefault("DB_PORT", "5432");
    private static final String DB_NAME     = System.getenv().getOrDefault("DB_NAME", "miner_db");
    private static final String DB_USER     = System.getenv().getOrDefault("DB_USER", "miner_user");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "miner_pass");

    private static final String DB_URL =
        String.format("jdbc:postgresql://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME);

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void initSchema() throws SQLException {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS repositories (
                    full_name TEXT PRIMARY KEY,
                    description TEXT,
                    html_url TEXT,
                    stars INTEGER,
                    size INTEGER,
                    language TEXT,
                    pushed_at TEXT,
                    forks INTEGER,
                    open_issues INTEGER,
                    commit_count INTEGER,
                    license TEXT,
                    topics TEXT,
                    watchers_count INTEGER,
                    has_issues_enabled BOOLEAN,
                    default_branch TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tech_profiles (
                    full_name TEXT PRIMARY KEY REFERENCES repositories(full_name),
                    build_tool TEXT,
                    framework TEXT,
                    java_version INTEGER,
                    java21 BOOLEAN,
                    graalvm_ready BOOLEAN,
                    has_test_suite BOOLEAN,
                    test_framework TEXT,
                    test_file_count INTEGER,
                    jmh_present BOOLEAN,
                    jmh_candidate BOOLEAN,
                    profiling_candidate BOOLEAN,
                    sector TEXT,
                    travis_ci BOOLEAN,
                    passes_hard_filters BOOLEAN
                )
            """);

            // "rank" se renombró a "repo_rank" — RANK() es palabra reservada
            // en SQL estándar (función de ventana), mejor evitar la ambigüedad
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS repo_scores (
                    full_name TEXT PRIMARY KEY REFERENCES repositories(full_name),
                    technical_score DOUBLE PRECISION,
                    test_quality_score DOUBLE PRECISION,
                    ci_hygiene_score DOUBLE PRECISION,
                    sector_score DOUBLE PRECISION,
                    popularity_score DOUBLE PRECISION,
                    maintenance_score DOUBLE PRECISION,
                    total_score DOUBLE PRECISION,
                    repo_rank INTEGER
                )
            """);
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS pipeline_stats (
                            id SERIAL PRIMARY KEY,
                            total_available INTEGER,
                            phase1_approved INTEGER,
                            phase2_approved INTEGER,
                            phase3_approved INTEGER,
                            scored_count INTEGER,
                            run_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);
        }
        System.out.println("✓ Esquema verificado/creado en PostgreSQL (" + DB_NAME + ")");
    }
}