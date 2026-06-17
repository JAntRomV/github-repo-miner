package com.miner;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class GraphQLSearchService {

    private static final String GRAPHQL_URL = "https://api.github.com/graphql";

    private final String token;
    private final SearchFilters filters;
    private final RepositoryValidator validator; // antes era SecondValidator
    private final Gson gson;

    public GraphQLSearchService(SearchFilters filters, RepositoryValidator validator) {
        this.token     = System.getenv("GITHUB_TOKEN");
        this.filters   = filters;
        this.validator = validator;
        this.gson      = new Gson();
    }

    // Ahora devuelve List<RepositoryData> en lugar de List<EnrichedRepositoryData>
    public List<RepositoryData> enrichAndFilter(List<RepositoryData> repos) {
        List<RepositoryData> enrichedList = new ArrayList<>();
        int total = repos.size();
        int processed = 0;

        System.out.println("\n========================================");
        System.out.println("  FASE 2: ENRIQUECIMIENTO CON GRAPHQL");
        System.out.println("  Repositorios a procesar: " + total);
        System.out.println("========================================");

        for (RepositoryData repo : repos) {
            processed++;
            System.out.println("\n[" + processed + "/" + total + "] " + repo.getFullName());

            RepositoryData enriched = fetchMetadata(repo); // antes devolvía EnrichedRepositoryData

            if (enriched != null && validator.validatePhase2(enriched, filters)) { // antes era validate()
                enrichedList.add(enriched);
            }

            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        validator.printPhase2Report(); // antes era printReport()
        return enrichedList;
    }

    private RepositoryData fetchMetadata(RepositoryData repo) {
        try {
            String[] parts = repo.getFullName().split("/");
            String owner = parts[0];
            String name  = parts[1];

            String graphqlQuery =
                "query($owner: String!, $name: String!) {" +
                "  repository(owner: $owner, name: $name) {" +
                "    defaultBranchRef {" +
                "      target {" +
                "        ... on Commit {" +
                "          history { totalCount }" +
                "        }" +
                "      }" +
                "    }" +
                "    licenseInfo { name spdxId }" +
                "    repositoryTopics(first: 10) {" +
                "      nodes { topic { name } }" +
                "    }" +
                "    watchers { totalCount }" +
                "    hasIssuesEnabled" +
                "  }" +
                "}";

            JsonObject variables = new JsonObject();
            variables.addProperty("owner", owner);
            variables.addProperty("name", name);

            JsonObject body = new JsonObject();
            body.addProperty("query", graphqlQuery);
            body.add("variables", variables);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GRAPHQL_URL))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

            HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("  [ERROR HTTP " + response.statusCode() + "]");
                return null;
            }

            return parseResponse(response.body(), repo);

        } catch (Exception e) {
            System.err.println("  [ERROR en petición] " + e.getMessage());
            return null;
        }
    }

    private RepositoryData parseResponse(String responseBody, RepositoryData repo) {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);

            if (json.has("errors")) {
                System.err.println("  [GraphQL Error] " + json.get("errors"));
                return null;
            }

            JsonObject repoData = json
                .getAsJsonObject("data")
                .getAsJsonObject("repository");

            if (repoData == null || repoData.isJsonNull()) {
                System.err.println("  [Error] Repositorio no encontrado en GraphQL");
                return null;
            }

            int commitCount = 0;
            JsonElement branchRef = repoData.get("defaultBranchRef");
            if (branchRef != null && !branchRef.isJsonNull()) {
                JsonElement target = branchRef.getAsJsonObject().get("target");
                if (target != null && !target.isJsonNull()) {
                    JsonElement history = target.getAsJsonObject().get("history");
                    if (history != null && !history.isJsonNull()) {
                        commitCount = history.getAsJsonObject().get("totalCount").getAsInt();
                    }
                }
            }

            String license = "No license";
            JsonElement licenseInfo = repoData.get("licenseInfo");
            if (licenseInfo != null && !licenseInfo.isJsonNull()) {
                license = licenseInfo.getAsJsonObject().get("name").getAsString();
            }

            List<String> topics = new ArrayList<>();
            JsonElement topicsEl = repoData.get("repositoryTopics");
            if (topicsEl != null && !topicsEl.isJsonNull()) {
                JsonArray nodes = topicsEl.getAsJsonObject().getAsJsonArray("nodes");
                for (JsonElement node : nodes) {
                    topics.add(node.getAsJsonObject()
                        .getAsJsonObject("topic")
                        .get("name").getAsString());
                }
            }

            int watchersCount = 0;
            JsonElement watchers = repoData.get("watchers");
            if (watchers != null && !watchers.isJsonNull()) {
                watchersCount = watchers.getAsJsonObject().get("totalCount").getAsInt();
            }

            boolean hasIssuesEnabled = false;
            JsonElement issuesEl = repoData.get("hasIssuesEnabled");
            if (issuesEl != null && !issuesEl.isJsonNull()) {
                hasIssuesEnabled = issuesEl.getAsBoolean();
            }

            // Ahora se enriquece el mismo objeto RepositoryData con los setters
            repo.setCommitCount(commitCount);
            repo.setLicense(license);
            repo.setTopics(topics);
            repo.setWatchersCount(watchersCount);
            repo.setHasIssuesEnabled(hasIssuesEnabled);

            System.out.println("  commits=" + commitCount
                + " | licencia=" + license
                + " | watchers=" + watchersCount
                + " | topics=" + topics);

            return repo;

        } catch (Exception e) {
            System.err.println("  [ERROR al parsear] " + e.getMessage());
            return null;
        }
    }
}