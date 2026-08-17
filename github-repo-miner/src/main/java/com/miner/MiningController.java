package com.miner;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
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