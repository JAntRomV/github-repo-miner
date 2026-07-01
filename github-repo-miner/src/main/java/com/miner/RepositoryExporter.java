package com.miner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.List;

public class RepositoryExporter {

    // Fase 1: exporta solo los campos básicos REST
    public void exportPhase1ToJson(List<RepositoryData> repos, String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), repos);
            System.out.println("JSON Fase 1 guardado: " + filename + " (" + repos.size() + " repos)");
        } catch (IOException e) {
            System.err.println("Error al guardar JSON: " + e.getMessage());
        }
    }

    public void exportPhase1ToCsv(List<RepositoryData> repos, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("fullName,description,url,stars,size,language,pushedAt,forks,openIssues");
            writer.newLine();
            for (RepositoryData repo : repos) {
                writer.write(String.format("%s,%s,%s,%d,%d,%s,%s,%d,%d",
                    sanitize(repo.getFullName()),
                    sanitize(repo.getDescription()),
                    sanitize(repo.getHtmlUrl()),
                    repo.getStars(),
                    repo.getSize(),
                    sanitize(repo.getLanguage()),
                    sanitize(repo.getPushedAt()),
                    repo.getForks(),
                    repo.getOpenIssues()
                ));
                writer.newLine();
            }
            System.out.println("CSV Fase 1 guardado: " + filename + " (" + repos.size() + " repos)");
        } catch (IOException e) {
            System.err.println("Error al guardar CSV: " + e.getMessage());
        }
    }

    // Fase 2: exporta todos los campos incluyendo los de GraphQL
    public void exportPhase2ToJson(List<RepositoryData> repos, String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), repos);
            System.out.println("JSON Fase 2 guardado: " + filename + " (" + repos.size() + " repos)");
        } catch (IOException e) {
            System.err.println("Error al guardar JSON Fase 2: " + e.getMessage());
        }
    }

    public void exportPhase2ToCsv(List<RepositoryData> repos, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("fullName,description,url,stars,size,language,pushedAt," +
                         "forks,openIssues,commitCount,license,topics,watchersCount,hasIssuesEnabled");
            writer.newLine();
            for (RepositoryData repo : repos) {
                String topicsJoined = repo.getTopics() != null
                    ? String.join("|", repo.getTopics()) : "";
                writer.write(String.format("%s,%s,%s,%d,%d,%s,%s,%d,%d,%d,%s,%s,%d,%b",
                    sanitize(repo.getFullName()),
                    sanitize(repo.getDescription()),
                    sanitize(repo.getHtmlUrl()),
                    repo.getStars(),
                    repo.getSize(),
                    sanitize(repo.getLanguage()),
                    sanitize(repo.getPushedAt()),
                    repo.getForks(),
                    repo.getOpenIssues(),
                    repo.getCommitCount(),
                    sanitize(repo.getLicense()),
                    sanitize(topicsJoined),
                    repo.getWatchersCount(),
                    repo.isHasIssuesEnabled()
                ));
                writer.newLine();
            }
            System.out.println("CSV Fase 2 guardado: " + filename + " (" + repos.size() + " repos)");
        } catch (IOException e) {
            System.err.println("Error al guardar CSV Fase 2: " + e.getMessage());
        }
    }

    private String sanitize(String value) {
        if (value == null) return "";
        return value.replace(",", ";").replace("\n", " ").replace("\r", "");
    }
    // fase 3 
    public void exportPhase3ToJson(List<RepositoryData> repos, String filename) {
    try {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), repos);
        System.out.println("JSON Fase 3 guardado: " + filename + " (" + repos.size() + " repos)");
    } catch (IOException e) {
        System.err.println("Error al guardar JSON Fase 3: " + e.getMessage());
    }
}

public void exportPhase3ToCsv(List<RepositoryData> repos, String filename) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {

        // Encabezado: 14 columnas de Fase 2 + 5 nuevas de Fase 3 = 19 columnas
        writer.write("fullName,description,url,stars,size,language,pushedAt,forks," +
                     "openIssues,commitCount,license,topics,watchersCount,hasIssuesEnabled," +
                     "buildTool,hasFrameworkDep,detectedFramework,detectedDeps,hasSrcMainJava");
        writer.newLine();

        for (RepositoryData repo : repos) {
            String topicsJoined = repo.getTopics() != null
                ? String.join("|", repo.getTopics()) : "";
            String depsJoined = repo.getDetectedDeps() != null
                ? String.join("|", repo.getDetectedDeps()) : "";

            writer.write(String.format("%s,%s,%s,%d,%d,%s,%s,%d,%d,%d,%s,%s,%d,%b,%s,%b,%s,%s,%b",
                sanitize(repo.getFullName()),
                sanitize(repo.getDescription()),
                sanitize(repo.getHtmlUrl()),
                repo.getStars(),
                repo.getSize(),
                sanitize(repo.getLanguage()),
                sanitize(repo.getPushedAt()),
                repo.getForks(),
                repo.getOpenIssues(),
                repo.getCommitCount(),
                sanitize(repo.getLicense()),
                sanitize(topicsJoined),
                repo.getWatchersCount(),
                repo.isHasIssuesEnabled(),
                sanitize(repo.getBuildTool()),
                repo.isHasFrameworkDependency(),
                sanitize(repo.getDetectedFramework()),
                sanitize(depsJoined),
                repo.isHasSrcMainJava()
            ));
            writer.newLine();
        }
        System.out.println("CSV Fase 3 guardado: " + filename + " (" + repos.size() + " repos)");

    } catch (IOException e) {
        System.err.println("Error al guardar CSV Fase 3: " + e.getMessage());
    }
}
}