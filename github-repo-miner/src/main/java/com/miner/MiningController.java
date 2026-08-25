package com.miner;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MiningController {

    private volatile String pipelineStatus = "idle";
    private volatile String scoringStatus = "idle";
    private volatile String storageStatus = "idle";

    // ═══════════════════════════════════════════
    // MINING (sin cambios)
    // ═══════════════════════════════════════════
    @PostMapping("/mining/run")
    public ResponseEntity<String> runMining() {
        if ("running".equals(pipelineStatus)) {
            return ResponseEntity.status(409).body("Ya hay un pipeline corriendo");
        }
        pipelineStatus = "running";
        new Thread(() -> {
            try {
                App.main(new String[]{});
                pipelineStatus = "completed";
            } catch (Exception e) {
                pipelineStatus = "error: " + e.getMessage();
            }
        }).start();
        return ResponseEntity.accepted().body("Pipeline iniciado en segundo plano");
    }

    @GetMapping("/mining/status")
    public ResponseEntity<String> miningStatus() {
        return ResponseEntity.ok(pipelineStatus);
    }

    // ═══════════════════════════════════════════
    // SCORING (sin cambios)
    // ═══════════════════════════════════════════
    @PostMapping("/scoring/run")
    public ResponseEntity<String> runScoring() {
        if ("running".equals(scoringStatus)) {
            return ResponseEntity.status(409).body("Ya hay un scoring corriendo");
        }
        scoringStatus = "running";
        new Thread(() -> {
            try {
                ScoringApp.main(new String[]{});
                scoringStatus = "completed";
            } catch (Exception e) {
                scoringStatus = "error: " + e.getMessage();
            }
        }).start();
        return ResponseEntity.accepted().body("Scoring iniciado en segundo plano");
    }

    @GetMapping("/scoring/status")
    public ResponseEntity<String> scoringStatus() {
        return ResponseEntity.ok(scoringStatus);
    }

    // ═══════════════════════════════════════════
    // STORAGE (sin cambios — este SÍ necesita Postgres)
    // ═══════════════════════════════════════════
    @PostMapping("/storage/run")
    public ResponseEntity<String> runStorage() {
        if ("running".equals(storageStatus)) {
            return ResponseEntity.status(409).body("Ya hay un storage corriendo");
        }
        storageStatus = "running";
        new Thread(() -> {
            try {
                StorageApp.main(new String[]{});
                storageStatus = "completed";
            } catch (Exception e) {
                storageStatus = "error: " + e.getMessage();
            }
        }).start();
        return ResponseEntity.accepted().body("Storage iniciado en segundo plano");
    }

    @GetMapping("/storage/status")
    public ResponseEntity<String> storageStatus() {
        return ResponseEntity.ok(storageStatus);
    }

    // ═══════════════════════════════════════════
    // PIPELINE STATS (nuevo — PARTE D.1)
    // ═══════════════════════════════════════════
    @GetMapping("/pipeline/stats")
    public ResponseEntity<Map<String, Object>> pipelineStats() {
        String sql = "SELECT * FROM pipeline_stats ORDER BY run_at DESC LIMIT 1";

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("totalAvailable", rs.getInt("total_available"));
                stats.put("phase1Approved", rs.getInt("phase1_approved"));
                stats.put("phase2Approved", rs.getInt("phase2_approved"));
                stats.put("phase3Approved", rs.getInt("phase3_approved"));
                stats.put("scoredCount", rs.getInt("scored_count"));
                return ResponseEntity.ok(stats);
            }
            return ResponseEntity.notFound().build();

        } catch (SQLException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════
    // RANKING COMPLETO (nuevo — PARTE D.2)
    // ═══════════════════════════════════════════
    @GetMapping("/ranking/all")
    public ResponseEntity<List<Map<String, Object>>> rankingAll() {
        String sql = """
            SELECT r.full_name, r.html_url, s.total_score, s.repo_rank
            FROM repo_scores s
            JOIN repositories r ON s.full_name = r.full_name
            ORDER BY s.repo_rank ASC
        """;

        List<Map<String, Object>> resultado = new ArrayList<>();

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> repo = new HashMap<>();
                repo.put("fullName", rs.getString("full_name"));
                repo.put("htmlUrl", rs.getString("html_url"));
                repo.put("totalScore", rs.getDouble("total_score"));
                repo.put("rank", rs.getInt("repo_rank"));
                resultado.add(repo);
            }
            return ResponseEntity.ok(resultado);

        } catch (SQLException e) {
            return ResponseEntity.status(500).body(List.of(Map.of("error", e.getMessage())));
        }
    }

    // ═══════════════════════════════════════════
    // EXPORT CSV (nuevo — PARTE D.3)
    // ═══════════════════════════════════════════
    @GetMapping("/export/csv")
    public ResponseEntity<String> exportCsv() {
        String sql = """
            SELECT r.full_name, r.html_url, r.stars, r.forks, t.framework,
                   t.java21, t.graalvm_ready, s.total_score, s.repo_rank
            FROM repo_scores s
            JOIN repositories r ON s.full_name = r.full_name
            JOIN tech_profiles t ON s.full_name = t.full_name
            ORDER BY s.repo_rank ASC
        """;

        StringBuilder csv = new StringBuilder();
        csv.append("rank,full_name,html_url,stars,forks,framework,java21,graalvm_ready,total_score\n");

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                csv.append(rs.getInt("repo_rank")).append(",")
                   .append(rs.getString("full_name")).append(",")
                   .append(rs.getString("html_url")).append(",")
                   .append(rs.getInt("stars")).append(",")
                   .append(rs.getInt("forks")).append(",")
                   .append(rs.getString("framework")).append(",")
                   .append(rs.getBoolean("java21")).append(",")
                   .append(rs.getBoolean("graalvm_ready")).append(",")
                   .append(rs.getDouble("total_score")).append("\n");
            }

            return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=repos_mineria.csv")
                .body(csv.toString());

        } catch (SQLException e) {
            return ResponseEntity.status(500).body("error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════
    // CATALOG (corregido para usar metricsStatus + endpoint nuevo)
    // ═══════════════════════════════════════════
    @GetMapping("/catalog/status")
    public ResponseEntity<Map<String, Long>> catalogStatus() {
        MongoCollection<Document> collection = MongoManager.getCatalogCollection();

        long pending = 0, complete = 0, failed = 0;

        for (Document doc : collection.find()) {
            String estado = calcularEstadoCombinado(doc);
            switch (estado) {
                case "complete" -> complete++;
                case "failed"   -> failed++;
                default         -> pending++;
            }
        }

        return ResponseEntity.ok(Map.of("pending", pending, "complete", complete, "failed", failed));
    }

    @GetMapping("/catalog/repos")
    public ResponseEntity<List<Map<String, String>>> catalogRepos() {
        MongoCollection<Document> collection = MongoManager.getCatalogCollection();

        List<Map<String, String>> repos = new ArrayList<>();

        for (Document doc : collection.find()) {
            String fullName = doc.getString("fullName");
            String estado = calcularEstadoCombinado(doc);
            repos.add(Map.of("_id", fullName, "status", estado));
        }

        return ResponseEntity.ok(repos);
    }

    // Deriva un único estado (pending/complete/failed) combinando
    // metricsStatus.static y metricsStatus.dynamic de cada documento
    private String calcularEstadoCombinado(Document doc) {
        Document metricsStatus = doc.get("metricsStatus", Document.class);
        if (metricsStatus == null) {
            return "pending";
        }

        String estatico = metricsStatus.getString("static");
        String dinamico = metricsStatus.getString("dynamic");

        if ("failed".equals(estatico) || "failed".equals(dinamico)) {
            return "failed";
        }
        if ("complete".equals(estatico) && "complete".equals(dinamico)) {
            return "complete";
        }
        return "pending";
    }

    // ═══════════════════════════════════════════
    // HEALTH (sin cambios)
    // ═══════════════════════════════════════════
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}