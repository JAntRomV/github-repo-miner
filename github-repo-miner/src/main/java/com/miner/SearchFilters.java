package com.miner;

import java.util.List;

public class SearchFilters {
    // Definimos los lenguajes objetivos solicitados
    private List<String> languages = List.of("Java", "Python", "JavaScript");
    
    // Delimitación inicial de calidad: Historial de mínimo 10 commits
    private int minCommits = 10; 
    
    // Filtros de frameworks técnicos requeridos
    private List<String> technicalFrameworks = List.of("SpringBoot", "Micronaut");

    public List<String> getLanguages() {
        return languages;
    }

    public int getMinCommits() {
        return minCommits;
    }

    public List<String> getTechnicalFrameworks() {
        return technicalFrameworks;
    }
}