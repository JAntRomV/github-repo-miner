package com.miner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.List;

public class StorageApp {
    public static void main(String[] args) throws Exception {
        System.out.println("=== ISSUE #4 PUNTO 2: Storage en Base de Datos ===\n");

        File phase3File = new File("results_phase3.json");
        File rankedFile  = new File("results_ranked.json");

        if (!phase3File.exists() || !rankedFile.exists()) {
            System.err.println("❌ Faltan archivos. Corre primero App y ScoringApp.");
            System.exit(1);
        }

        ObjectMapper mapper = new ObjectMapper();
        List<RepositoryData> repos  = List.of(mapper.readValue(phase3File, RepositoryData[].class));
        List<RepoScore> scores      = List.of(mapper.readValue(rankedFile, RepoScore[].class));

        System.out.println("Repos cargados: " + repos.size());
        System.out.println("Scores cargados: " + scores.size());

        DatabaseManager.initSchema();

        RepositoryStorageService storageService = new RepositoryStorageService();
        storageService.saveAll(repos, scores);

        System.out.println("\n✅ Storage completo. Archivo generado: miner.db");
    }
}
