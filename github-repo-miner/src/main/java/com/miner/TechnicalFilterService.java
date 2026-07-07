package com.miner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TechnicalFilterService {

    private static final String API_BASE = "https://api.github.com/repos";

    private final String token;
    private final RepositoryValidator validator;
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final RawFileFetcher rawFetcher = new RawFileFetcher();
    private final List<BuildFileAnalyzer> analyzers = List.of(new MavenAnalyzer(), new GradleAnalyzer());

    public TechnicalFilterService(SearchFilters filters, RepositoryValidator validator) {
        this.token = System.getenv("GITHUB_TOKEN");
        this.validator = validator;
    }

    public List<RepositoryData> filterAndValidate(List<RepositoryData> repos) {
        List<RepositoryData> approved = new ArrayList<>();
        int total = repos.size();
        int processed = 0;

        System.out.println("\n========================================");
        System.out.println("  FASE 3 v2: TechProfile");
        System.out.println("  Repositorios a analizar: " + total);
        System.out.println("========================================");

        for (RepositoryData repo : repos) {
            processed++;
            String[] parts = repo.getFullName().split("/");
            String owner = parts[0];
            String name  = parts[1];
            String branch = (repo.getDefaultBranch() != null && !repo.getDefaultBranch().isEmpty())
                ? repo.getDefaultBranch() : "main"; // fallback si Fase 2 no lo capturó

            System.out.println("\n[" + processed + "/" + total + "] " + repo.getFullName() + " (branch=" + branch + ")");

            try {
                // 1. Inventario — UNA sola llamada a Git Trees API
                FileTree tree = fetchFileTree(owner, name, branch);
                if (tree.isTruncated()) {
                    System.out.println("  [WARN] Árbol truncado (repo muy grande)");
                }

                // 2. Build tool SIN descargar contenido
                BuildTool tool = tree.detectBuildTool();

                // 3. Descargar SOLO los build files relevantes
                Map<String, String> buildFiles = downloadRelevantFiles(owner, name, branch, tool, tree);

                // 4. Seleccionar analyzer (patrón Strategy)
                BuildFileAnalyzer analyzer = analyzers.stream()
                    .filter(a -> a.supports(tool)).findFirst().orElse(null);

                TechProfile profile = (analyzer != null)
                    ? analyzer.analyze(tree, buildFiles)
                    : TechProfile.empty(tool);

                // 5. Refinar sector con topics de Fase 2
                profile = refineSector(profile, repo);

                repo.setTechProfile(profile);
                System.out.println("  [PROFILE] " + summarize(profile));

                // 6. Filtros duros
                if (validator.validatePhase3(profile)) {
                    approved.add(repo);
                }

            } catch (Exception e) {
                System.err.println("  [ERROR] " + repo.getFullName() + ": " + e.getMessage());
            }

            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        }

        validator.printPhase3Report();
        return approved;
    }

    // 1 llamada por repo — inventario completo
    private FileTree fetchFileTree(String owner, String repo, String branch) throws Exception {
        String url = String.format("%s/%s/%s/git/trees/%s?recursive=1", API_BASE, owner, repo, branch);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github.v3+json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("HTTP " + response.statusCode() + " obteniendo árbol");
        }

        return FileTree.fromJson(gson.fromJson(response.body(), JsonObject.class));
    }

    // Descarga SOLO los build files relevantes, vía raw.githubusercontent.com
    private Map<String, String> downloadRelevantFiles(String owner, String repo, String branch,
                                                        BuildTool tool, FileTree tree) {
        Map<String, String> files = new HashMap<>();

        if (tool == BuildTool.MAVEN) {
            rawFetcher.fetch(owner, repo, branch, "pom.xml")
                .ifPresent(c -> files.put("pom.xml", c));

        } else if (tool == BuildTool.GRADLE) {
            String primary = tree.getPrimaryBuildFilePath();
            if (primary != null) {
                rawFetcher.fetch(owner, repo, branch, primary).ifPresent(c -> files.put(primary, c));
            }
            if (tree.fileExists("gradle/libs.versions.toml")) {
                rawFetcher.fetch(owner, repo, branch, "gradle/libs.versions.toml")
                    .ifPresent(c -> files.put("gradle/libs.versions.toml", c));
            }
            if (tree.fileExists("gradle.properties")) {
                rawFetcher.fetch(owner, repo, branch, "gradle.properties")
                    .ifPresent(c -> files.put("gradle.properties", c));
            }
        }
        return files;
    }

    // Refina Sector con la señal que SÍ tenemos de Fase 2: topics
    private TechProfile refineSector(TechProfile profile, RepositoryData repo) {
        if (profile.sector() != Sector.UNKNOWN) return profile; // ya venía de CITATION.cff

        boolean looksAcademic = repo.getTopics() != null && repo.getTopics().stream()
            .anyMatch(t -> t.contains("research") || t.contains("academic") || t.contains("university"));

        if (!looksAcademic) return profile;

        return new TechProfile(
            profile.buildTool(), profile.framework(), profile.javaVersion(), profile.java21(),
            profile.graalvmReady(), profile.hasTestSuite(), profile.testFramework(), profile.testFileCount(),
            profile.jmhPresent(), profile.jmhCandidate(), profile.profilingCandidate(),
            Sector.ACADEMIC, profile.travisCi(), profile.passesHardFilters()
        );
    }

    private String summarize(TechProfile p) {
        return p.framework() + " | Java" + p.javaVersion() + (p.java21() ? "✓" : "✗")
            + " | GraalVM=" + p.graalvmReady() + " | Tests=" + p.hasTestSuite()
            + " (" + p.testFileCount() + ") | JMH=" + p.jmhCandidate()
            + " | HardFilters=" + p.passesHardFilters();
    }
}