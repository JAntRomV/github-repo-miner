package com.miner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.List;

public class RepositoryExporter {

    public void exportToJson(List<RepositoryData> repos, String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), repos);
            System.out.println("JSON guardado: " + filename + " (" + repos.size() + " repositorios)");
        } catch (IOException e) {
            System.err.println("Error al guardar JSON: " + e.getMessage());
        }
    }

    public void exportToCsv(List<RepositoryData> repos, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {

            // Encabezado
            writer.write("fullName,description,url,stars,size,language,pushedAt,forks,openIssues");
            writer.newLine();

            // Filas
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
            System.out.println("CSV guardado: " + filename + " (" + repos.size() + " repositorios)");

        } catch (IOException e) {
            System.err.println("Error al guardar CSV: " + e.getMessage());
        }
    }

    // Evita que comas o saltos de línea rompan el formato CSV
    private String sanitize(String value) {
        if (value == null) return "";
        return value.replace(",", ";").replace("\n", " ").replace("\r", "");
    }
}