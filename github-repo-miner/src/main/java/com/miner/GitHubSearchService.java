package com.miner;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedSearchIterable;
import java.util.ArrayList;
import java.util.List;

public class GitHubSearchService {

    private SearchFilters filters;
    private RepositoryValidator validator;
    
    // C.1 — Campo acumulador para guardar el total reportado por GitHub
    private int totalReportadoPorGithub = 0;

    public GitHubSearchService(SearchFilters filters, RepositoryValidator validator) {
        this.filters   = filters;
        this.validator = validator;
    }

    public List<RepositoryData> executePipeline() {
        try {
            String miToken = System.getenv("GITHUB_TOKEN");

            if (miToken == null || miToken.isEmpty()) {
                throw new IllegalStateException("Variable de entorno GITHUB_TOKEN no configurada.");
            }

            GitHub github = new GitHubBuilder().withOAuthToken(miToken).build();

            List<RepositoryData> allValidatedRepos = new ArrayList<>();

            for (String topic : filters.getTechnicalFrameworks()) {

                // Query explicado filtro por filtro
                String query = "topic:"     + topic        // Framework objetivo
                             + " language:Java"            // Solo Java
                             + " stars:>30"                // Mínimo de popularidad
                             + " pushed:>2025-06-01"       // Actividad reciente
                             + " archived:false"           // Excluir repos archivados
                             + " -is:template";            // Excluir repos plantilla

                System.out.println("\n========================================");
                System.out.println("  Topic buscado:  " + topic);
                System.out.println("  Query enviada:  " + query);
                System.out.println("========================================");

                PagedSearchIterable<GHRepository> searchResults =
                    github.searchRepositories().q(query).list();

                int total = searchResults.getTotalCount();
                
                // C.1 — Acumular el total por cada topic consultado
                totalReportadoPorGithub += total;

                System.out.println("  [API] Total reportado por GitHub: " + total);
                System.out.println("  [Validando cada resultado...]\n");

                // Resetear contadores para este topic
                validator.resetPhase1Counters(); 

                for (GHRepository repo : searchResults) {
                    boolean passed = validator.validatePhase1(repo, filters);
                    if (passed) {
                        allValidatedRepos.add(RepositoryData.fromGHRepository(repo));
                    }
                }

                // Reporte de cuántos pasaron cada filtro para este topic
                validator.printPhase1Report(); 
            }

            System.out.println("\n========================================");
            System.out.println("  TOTAL FINAL VALIDADOS: " + allValidatedRepos.size());
            System.out.println("========================================\n");

            // Exportar resultados
            RepositoryExporter exporter = new RepositoryExporter();
            exporter.exportPhase1ToJson(allValidatedRepos, "results.json");
            exporter.exportPhase1ToCsv(allValidatedRepos,  "results.csv");
            return allValidatedRepos;

        } catch (Exception e) {
            System.err.println("Ocurrió un error en el flujo de la API: " + e.getMessage());
            return new ArrayList<>();  // <-- agregar este return
        }
    }

    // C.1 — Getter público para obtener el acumulado total
    public int getTotalReportadoPorGithub() {
        return totalReportadoPorGithub;
    }
}