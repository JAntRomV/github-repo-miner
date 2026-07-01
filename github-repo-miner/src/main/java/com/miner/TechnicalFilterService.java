package com.miner;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class TechnicalFilterService {

    private static final String API_BASE = "https://api.github.com/repos";

    private final String              token;
    private final SearchFilters       filters;
    private final RepositoryValidator validator;
    private final RepositoryParser    parser;
    private final Gson                gson;
    private final HttpClient          httpClient;

    public TechnicalFilterService(SearchFilters filters, RepositoryValidator validator) {
        this.token      = System.getenv("GITHUB_TOKEN");
        this.filters    = filters;
        this.validator  = validator;
        this.parser     = new RepositoryParser();
        this.gson       = new Gson();
        this.httpClient = HttpClient.newHttpClient();
    }

    // Orquestador de Fase 3
    public List<RepositoryData> filterAndValidate(List<RepositoryData> repos) {
        List<RepositoryData> approvedRepos = new ArrayList<>();
        int total     = repos.size();
        int processed = 0;

        System.out.println("\n========================================");
        System.out.println("  FASE 3: FILTRO TECNICO");
        System.out.println("  Repositorios a analizar: " + total);
        System.out.println("========================================");

        for (RepositoryData repo : repos) {
            processed++;
            System.out.println("\n[" + processed + "/" + total + "] " + repo.getFullName());

            String[] parts = repo.getFullName().split("/");
            String owner = parts[0];
            String name  = parts[1];

            // 1. Intentar descargar el archivo de build
            ParseResult parseResult = fetchAndParseBuildFile(owner, name);

            // 2. Verificar si existe la estructura estándar de Java
            boolean hasSrcMainJava = checkDirectoryExists(owner, name, "src/main/java");

            // 3. Llenar campos de Fase 3 en RepositoryData
            repo.setBuildTool(parseResult.getBuildTool());
            repo.setHasFrameworkDependency(
                parseResult.isHasSpringBoot() || parseResult.isHasMicronaut()
            );
            repo.setDetectedFramework(resolveFramework(parseResult));
            repo.setDetectedDeps(parseResult.getDetectedDeps());
            repo.setHasSrcMainJava(hasSrcMainJava);

            System.out.println("  [PARSE] buildTool="    + repo.getBuildTool()
                + " | framework="  + repo.getDetectedFramework()
                + " | deps="       + repo.getDetectedDeps()
                + " | src/main/java=" + hasSrcMainJava);

            // 4. Aplicar la tercera validación
            if (validator.validatePhase3(repo)) {
                approvedRepos.add(repo);
            }

            // Pausa para respetar rate limits de la API
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        }

        validator.printPhase3Report();
        return approvedRepos;
    }

    // Intenta descargar pom.xml → build.gradle → build.gradle.kts en ese orden
    private ParseResult fetchAndParseBuildFile(String owner, String repo) {

        // Intento 1: pom.xml (Maven)
        String pomContent = fetchFileContent(owner, repo, "pom.xml");
        if (pomContent != null) {
            System.out.println("  [BUILD] pom.xml encontrado → Maven");
            return parser.parsePomXml(pomContent);
        }

        // Intento 2: build.gradle (Gradle Groovy DSL)
        String gradleContent = fetchFileContent(owner, repo, "build.gradle");
        if (gradleContent != null) {
            System.out.println("  [BUILD] build.gradle encontrado → Gradle");
            return parser.parseBuildGradle(gradleContent);
        }

        // Intento 3: build.gradle.kts (Gradle Kotlin DSL)
        String gradleKtsContent = fetchFileContent(owner, repo, "build.gradle.kts");
        if (gradleKtsContent != null) {
            System.out.println("  [BUILD] build.gradle.kts encontrado → Gradle Kotlin");
            return parser.parseBuildGradle(gradleKtsContent);
        }

        // No se encontró ningún archivo de build
        System.out.println("  [BUILD] Sin archivo de build en la raiz del repo");
        return new ParseResult(); // buildTool = "None" por defecto
    }

    // Descarga un archivo desde la API de GitHub y decodifica el contenido Base64
    private String fetchFileContent(String owner, String repo, String filename) {
        try {
            String url = API_BASE + "/" + owner + "/" + repo + "/contents/" + filename;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json    = gson.fromJson(response.body(), JsonObject.class);
                JsonElement content = json.get("content");

                if (content != null && !content.isJsonNull()) {
                    // GitHub devuelve el contenido en Base64 con saltos de línea — hay que limpiarlos
                    String base64Clean = content.getAsString()
                        .replace("\n", "")
                        .replace("\\n", "");
                    byte[] decoded = Base64.getDecoder().decode(base64Clean);
                    return new String(decoded, StandardCharsets.UTF_8);
                }
            }
            return null; // 404 = archivo no existe en este repo

        } catch (Exception e) {
            System.err.println("  [ERROR] fetchFileContent(" + filename + "): " + e.getMessage());
            return null;
        }
    }

    // Verifica si un directorio existe en el repo usando la API de contenidos
    private boolean checkDirectoryExists(String owner, String repo, String path) {
        try {
            String url = API_BASE + "/" + owner + "/" + repo + "/contents/" + path;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200; // 200 = existe, 404 = no existe

        } catch (Exception e) {
            return false;
        }
    }

    // Determina qué framework fue detectado según el resultado del parseo
    private String resolveFramework(ParseResult result) {
        if (result.isHasSpringBoot() && result.isHasMicronaut()) return "Both";
        if (result.isHasSpringBoot()) return "Spring Boot";
        if (result.isHasMicronaut())  return "Micronaut";
        return "None";
    }
}