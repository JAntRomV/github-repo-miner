package com.miner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.List;

/**
 * Entry point independiente de la Fase 4.
 * Lee results_phase3.json (ya generado por el pipeline completo)
 * y NO vuelve a tocar la API de GitHub — corre en segundos.
 */
public class ScoringApp {
    public static void main(String[] args) throws Exception {
        System.out.println("=== ISSUE #4: Scoring y Ranking (modo independiente) ===\n");

        File inputFile = new File("results_phase3.json");
        if (!inputFile.exists()) {
            System.err.println("❌ No se encontró results_phase3.json en el directorio actual.");
            System.err.println("   Corre primero el pipeline completo: mvn exec:java -Dexec.mainClass=\"com.miner.App\"");
            System.exit(1);
        }

        ObjectMapper mapper = new ObjectMapper();
        RepositoryData[] repoArray = mapper.readValue(inputFile, RepositoryData[].class);
        List<RepositoryData> repos = List.of(repoArray);

        System.out.println("Repos cargados desde results_phase3.json: " + repos.size());

        ScoringEngine engine = new ScoringEngine();
        List<RepoScore> ranked = engine.scoreAndRank(repos);

        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(new File("results_ranked.json"), ranked);

        System.out.println("\n✅ Ranking guardado en: results_ranked.json");
        System.out.println("   Total repos puntuados: " + ranked.size());
    }
}