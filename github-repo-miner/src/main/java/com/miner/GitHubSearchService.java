package com.miner;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedSearchIterable;
import org.kohsuke.github.GHRepository;
import java.io.IOException;

public class GitHubSearchService {
    private GitHub github;

    public GitHubSearchService() throws IOException {
        // Inicializa la conexión con GitHub usando variables de entorno o de forma anónima
        this.github = GitHubBuilder.fromEnvironment().build();
    }

    public PagedSearchIterable<GHRepository> searchRepositories(SearchFilters filters) {
        // Construimos un query básico inicial en base a los filtros (ejemplo: buscar proyectos Java)
        String query = "language:" + filters.getLanguages().get(0); 
        
        System.out.println("🤖 Ejecutando búsqueda en API de GitHub usando query: " + query);
        return github.searchRepositories().q(query).list();
    }
}