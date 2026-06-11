package com.miner;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedSearchIterable;

public class App {
    public static void main(String[] args) {
        try {
            System.out.println("=== INICIANDO FRAMEWORK DE MINERÍA (ISSUE #1) ===");
            
            // Punto 1: Establecer los criterios del set de filtros
            SearchFilters filters = new SearchFilters();
            
            // Punto 2: Invocar la herramienta de búsqueda de la API
            GitHubSearchService searchService = new GitHubSearchService();
            PagedSearchIterable<GHRepository> searchResults = searchService.searchRepositories(filters);
            
            // Punto 3: Instanciar el validador de contenido inicial
            InitialValidator validator = new InitialValidator();
            
            // Probamos procesando una muestra de los primeros 5 resultados devueltos por la API
            System.out.println("--- Analizando las primeras respuestas de la API ---");
            int count = 0;
            for (GHRepository repo : searchResults) {
                if (count >= 5) break; 
                
                // Aplicamos la validación
                validator.validate(repo, filters);
                count++;
            }
            
            System.out.println("=== PROCESO FINALIZADO CON ÉXITO ===");
            
        } catch (Exception e) {
            System.err.println("Ocurrió un error en el flujo de la API: " + e.getMessage());
        }
    }
}