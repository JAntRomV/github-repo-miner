package com.miner;

import java.util.List;

public class SearchFilters {

    private List<String> languages = List.of("Java", "Python", "JavaScript");
    private int minCommits = 10;
    // Valores en minúscula para coincidir con los topics reales de GitHub
    private List<String> technicalFrameworks = List.of("micronaut", "spring-boot");

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