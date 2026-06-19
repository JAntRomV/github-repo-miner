package com.miner;

import java.util.List;

public class App {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO FRAMEWORK DE MINERÍA ===");

        SearchFilters filters = new SearchFilters();

        // Un solo validator para ambas fases
        RepositoryValidator validator = new RepositoryValidator();

        // --- FASE 1 ---
        GitHubSearchService searchService = new GitHubSearchService(filters, validator);
        List<RepositoryData> phase1Results = searchService.executePipeline();

        // --- FASE 2 ---
        GraphQLSearchService graphqlService = new GraphQLSearchService(filters, validator);
        List<RepositoryData> phase2Results = graphqlService.enrichAndFilter(phase1Results);

        // Exportar Fase 2
        RepositoryExporter exporter = new RepositoryExporter();
        exporter.exportPhase2ToJson(phase2Results, "results_phase2.json");
        exporter.exportPhase2ToCsv(phase2Results,  "results_phase2.csv");

        System.out.println("\n=== PROCESO FINALIZADO CON ÉXITO ===");
    }
}