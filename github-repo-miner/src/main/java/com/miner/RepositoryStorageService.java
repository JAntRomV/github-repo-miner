package com.miner;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RepositoryStorageService {

    private final Gson gson = new Gson();

    public void saveAll(List<RepositoryData> repos, List<RepoScore> scores) throws SQLException {
        Map<String, RepoScore> scoreByName = scores.stream()
            .collect(Collectors.toMap(RepoScore::fullName, s -> s));

        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);

            for (RepositoryData repo : repos) {
                saveRepository(conn, repo);
                saveTechProfile(conn, repo);

                RepoScore score = scoreByName.get(repo.getFullName());
                if (score != null) {
                    saveScore(conn, score);
                } else {
                    System.out.println("  [WARN] Sin score para: " + repo.getFullName());
                }
            }

            conn.commit();
            System.out.println("✓ " + repos.size() + " repositorios guardados en PostgreSQL");
        }
    }

    private void saveRepository(Connection conn, RepositoryData repo) throws SQLException {
        String sql = """
            INSERT INTO repositories
            (full_name, description, html_url, stars, size, language, pushed_at,
             forks, open_issues, commit_count, license, topics, watchers_count,
             has_issues_enabled, default_branch)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (full_name) DO UPDATE SET
                description=excluded.description, stars=excluded.stars,
                size=excluded.size, forks=excluded.forks,
                open_issues=excluded.open_issues, commit_count=excluded.commit_count,
                license=excluded.license, topics=excluded.topics,
                watchers_count=excluded.watchers_count,
                has_issues_enabled=excluded.has_issues_enabled,
                pushed_at=excluded.pushed_at
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, repo.getFullName());
            ps.setString(2, repo.getDescription());
            ps.setString(3, repo.getHtmlUrl());
            ps.setInt(4, repo.getStars());
            ps.setInt(5, repo.getSize());
            ps.setString(6, repo.getLanguage());
            ps.setString(7, repo.getPushedAt());
            ps.setInt(8, repo.getForks());
            ps.setInt(9, repo.getOpenIssues());
            ps.setInt(10, repo.getCommitCount());
            ps.setString(11, repo.getLicense());
            ps.setString(12, gson.toJson(repo.getTopics()));
            ps.setInt(13, repo.getWatchersCount());
            ps.setBoolean(14, repo.isHasIssuesEnabled());   // antes: setInt(...,  ? 1 : 0)
            ps.setString(15, repo.getDefaultBranch());
            ps.executeUpdate();
        }
    }

    private void saveTechProfile(Connection conn, RepositoryData repo) throws SQLException {
        TechProfile p = repo.getTechProfile();
        if (p == null) return;

        String sql = """
            INSERT INTO tech_profiles
            (full_name, build_tool, framework, java_version, java21, graalvm_ready,
             has_test_suite, test_framework, test_file_count, jmh_present,
             jmh_candidate, profiling_candidate, sector, travis_ci, passes_hard_filters)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (full_name) DO UPDATE SET
                build_tool=excluded.build_tool, framework=excluded.framework,
                java_version=excluded.java_version, java21=excluded.java21,
                graalvm_ready=excluded.graalvm_ready, has_test_suite=excluded.has_test_suite,
                test_framework=excluded.test_framework, test_file_count=excluded.test_file_count,
                jmh_present=excluded.jmh_present, jmh_candidate=excluded.jmh_candidate,
                profiling_candidate=excluded.profiling_candidate, sector=excluded.sector,
                travis_ci=excluded.travis_ci, passes_hard_filters=excluded.passes_hard_filters
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, repo.getFullName());
            ps.setString(2, p.buildTool().name());
            ps.setString(3, p.framework().name());
            ps.setInt(4, p.javaVersion());
            ps.setBoolean(5, p.java21());
            ps.setBoolean(6, p.graalvmReady());
            ps.setBoolean(7, p.hasTestSuite());
            ps.setString(8, p.testFramework().name());
            ps.setInt(9, p.testFileCount());
            ps.setBoolean(10, p.jmhPresent());
            ps.setBoolean(11, p.jmhCandidate());
            ps.setBoolean(12, p.profilingCandidate());
            ps.setString(13, p.sector().name());
            ps.setBoolean(14, p.travisCi());
            ps.setBoolean(15, p.passesHardFilters());
            ps.executeUpdate();
        }
    }

    private void saveScore(Connection conn, RepoScore s) throws SQLException {
        String sql = """
            INSERT INTO repo_scores
            (full_name, technical_score, test_quality_score, ci_hygiene_score,
             sector_score, popularity_score, maintenance_score, total_score, repo_rank)
            VALUES (?,?,?,?,?,?,?,?,?)
            ON CONFLICT (full_name) DO UPDATE SET
                technical_score=excluded.technical_score,
                test_quality_score=excluded.test_quality_score,
                ci_hygiene_score=excluded.ci_hygiene_score,
                sector_score=excluded.sector_score,
                popularity_score=excluded.popularity_score,
                maintenance_score=excluded.maintenance_score,
                total_score=excluded.total_score,
                repo_rank=excluded.repo_rank
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.fullName());
            ps.setDouble(2, s.technicalScore());
            ps.setDouble(3, s.testQualityScore());
            ps.setDouble(4, s.ciHygieneScore());
            ps.setDouble(5, s.sectorScore());
            ps.setDouble(6, s.popularityScore());
            ps.setDouble(7, s.maintenanceScore());
            ps.setDouble(8, s.totalScore());
            ps.setInt(9, s.rank());
            ps.executeUpdate();
        }
    }
}