package com.miner;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedSearchIterable;

public class GitHubSearchService {

    private SearchFilters filters;
    private InitialValidator validator;

    public GitHubSearchService(SearchFilters filters, InitialValidator validator) {
        this.filters = filters;
        this.validator = validator;
    }

    public void executePipeline() {
        try {
            String miToken = System.getenv("GITHUB_TOKEN");

            if (miToken == null || miToken.isEmpty()) {
                throw new IllegalStateException("Variable de entorno GITHUB_TOKEN no configurada.");
            }

            GitHub github = new GitHubBuilder().withOAuthToken(miToken).build();

            // La API de GitHub no soporta OR entre qualifiers como me pide
            // se ejecuta una búsqueda independiente por cada framework
            for (String topic : filters.getTechnicalFrameworks()) {
                String query = "topic:" + topic +
                               " language:Java" +
                               " stars:>30" +
                               " pushed:>2025-06-01" +
                               " archived:false" +
                               " -is:template";

                System.out.println("\n--- Buscando repositorios con topic: " + topic + " ---");

                PagedSearchIterable<GHRepository> searchResults =
                    github.searchRepositories().q(query).list();

                int count = 0;
                for (GHRepository repo : searchResults) {
                    if (count >= 5) break;
                    validator.validate(repo, filters);
                    count++;
                }
            }

        } catch (Exception e) {
            System.err.println("Ocurrió un error en el flujo de la API: " + e.getMessage());
        }
    }
}