package com.miner;

import java.util.List;

public class App {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO FRAMEWORK DE MINERIA ===");

        SearchFilters filters         = new SearchFilters();
        RepositoryValidator validator = new RepositoryValidator();

        // FASE 1: REST API
        GitHubSearchService searchService = new GitHubSearchService(filters, validator);
        List<RepositoryData> phase1Results = searchService.executePipeline();

        // FASE 2: GraphQL
        GraphQLSearchService graphqlService = new GraphQLSearchService(filters, validator);
        List<RepositoryData> phase2Results = graphqlService.enrichAndFilter(phase1Results);

        // FASE 3: Filtro Técnico
        TechnicalFilterService technicalService = new TechnicalFilterService(filters, validator);
        List<RepositoryData> phase3Results = technicalService.filterAndValidate(phase2Results);

        // Exportar resultado final de Fase 3
        RepositoryExporter exporter = new RepositoryExporter();
        exporter.exportPhase3ToJson(phase3Results, "results_phase3.json");
        exporter.exportPhase3ToCsv(phase3Results,  "results_phase3.csv");

        // Guardar estadísticas del pipeline en Postgres (para el dashboard)
        try {
            int totalGithub = searchService.getTotalReportadoPorGithub();

            PipelineStatsService statsService = new PipelineStatsService();
            statsService.save(
                totalGithub,
                phase1Results.size(),
                phase2Results.size(),
                phase3Results.size(),
                phase3Results.size()
            );
        } catch (Exception e) {
            System.err.println("⚠ No se pudieron guardar las estadísticas del pipeline: " + e.getMessage());
        }

        System.out.println("\n=== PROCESO FINALIZADO CON EXITO ===");
    }
}