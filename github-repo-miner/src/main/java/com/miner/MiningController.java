package com.miner;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MiningController {

    private volatile String pipelineStatus = "idle";
    private volatile String scoringStatus = "idle";
    private volatile String storageStatus = "idle";

    // ═══════════════════════════════════════════
    // MINING (ya existente, sin cambios)
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
    // SCORING (nuevo)
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
    // STORAGE (nuevo — este SÍ necesita Postgres)
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
    // CATALOG (ya existente, sin cambios)
    // ═══════════════════════════════════════════
    @GetMapping("/catalog/status")
    public ResponseEntity<Map<String, Long>> catalogStatus() {
        MongoCollection<Document> collection = MongoManager.getCatalogCollection();
        long pending = collection.countDocuments(Filters.eq("status", "pending_metrics"));
        long complete = collection.countDocuments(Filters.eq("status", "metrics_complete"));
        long failed = collection.countDocuments(Filters.eq("status", "metrics_failed"));
        return ResponseEntity.ok(Map.of("pending", pending, "complete", complete, "failed", failed));
    }

    // ═══════════════════════════════════════════
    // HEALTH (ya existente, sin cambios)
    // ═══════════════════════════════════════════
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}