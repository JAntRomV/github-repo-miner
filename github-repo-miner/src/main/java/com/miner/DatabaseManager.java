package com.miner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:miner.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
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
                    has_issues_enabled INTEGER,
                    default_branch TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tech_profiles (
                    full_name TEXT PRIMARY KEY,
                    build_tool TEXT,
                    framework TEXT,
                    java_version INTEGER,
                    java21 INTEGER,
                    graalvm_ready INTEGER,
                    has_test_suite INTEGER,
                    test_framework TEXT,
                    test_file_count INTEGER,
                    jmh_present INTEGER,
                    jmh_candidate INTEGER,
                    profiling_candidate INTEGER,
                    sector TEXT,
                    travis_ci INTEGER,
                    passes_hard_filters INTEGER,
                    FOREIGN KEY (full_name) REFERENCES repositories(full_name)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS repo_scores (
                    full_name TEXT PRIMARY KEY,
                    technical_score REAL,
                    test_quality_score REAL,
                    ci_hygiene_score REAL,
                    sector_score REAL,
                    popularity_score REAL,
                    maintenance_score REAL,
                    total_score REAL,
                    rank INTEGER,
                    FOREIGN KEY (full_name) REFERENCES repositories(full_name)
                )
            """);
        }
        System.out.println("✓ Esquema verificado/creado en miner.db");
    }
}