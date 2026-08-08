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

    @GetMapping("/catalog/status")
    public ResponseEntity<Map<String, Long>> catalogStatus() {
        MongoCollection<Document> collection = MongoManager.getCatalogCollection();
        long pending = collection.countDocuments(Filters.eq("status", "pending_metrics"));
        long complete = collection.countDocuments(Filters.eq("status", "metrics_complete"));
        long failed = collection.countDocuments(Filters.eq("status", "metrics_failed"));
        return ResponseEntity.ok(Map.of("pending", pending, "complete", complete, "failed", failed));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}