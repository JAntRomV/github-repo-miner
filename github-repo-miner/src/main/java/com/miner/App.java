package com.miner;

public class App {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO FRAMEWORK DE MINERÍA (ISSUE #1) ===");
        
        // 1. Instanciamos los componentes especializados
        SearchFilters filters = new SearchFilters();
        InitialValidator validator = new InitialValidator();
        
        // 2. Acoplamos los componentes en el servicio orquestador
        GitHubSearchService searchService = new GitHubSearchService(filters, validator);
        
        // 3. Arrancamos el motor lógico de la Fase 1 sin amontonar código aquí
        searchService.executePipeline();
        
        System.out.println("=== PROCESO FINALIZADO CON ÉXITO ===");
    }
}