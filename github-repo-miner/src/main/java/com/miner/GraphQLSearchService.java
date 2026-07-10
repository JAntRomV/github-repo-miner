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

/**
 * Servicio encargado de la Fase 2: Enriquecimiento y filtrado de datos mediante GraphQL.
 * Conecta con la API v4 de GitHub para extraer información detallada (commits, licencias, 
 * watchers y topics) en una sola petición HTTP por repositorio, optimizando el consumo de red.
 */
public class GraphQLSearchService {

    // punto de acceso para las consultaas de GrapQL de GitHub
    private static final String GRAPHQL_URL = "https://api.github.com/graphql";

    private final String token;
    private final SearchFilters filters;
    private final RepositoryValidator validator; 
    private final Gson gson;

    /**
     * Constructor del servicio que inicializa las dependencias y recupera el token de seguridad.
     * @param filters Criterios de filtrado global (commits mínimos, licencias válidas).
     * @param validator Componente central de validación y contadores estadísticos.
     */
    public GraphQLSearchService(SearchFilters filters, RepositoryValidator validator) {
        // Recupera el token personal de acceso (PAT) guardado en el sistema operativo
        this.token     = System.getenv("GITHUB_TOKEN");
        this.filters   = filters;
        this.validator = validator;
        this.gson      = new Gson();
    }

    /**
     * Orquestador de la Fase 2. Toma la lista de repositorios aprobados en la Fase 1 (REST API),
     * consulta sus datos avanzados vía GraphQL y aplica los filtros de commits mínimos y licencias válidas.
     * @param repos Lista de repositorios pre-filtrados en la Fase 1.
     * @return Lista de repositorios enriquecidos que aprobaron las validaciones de Fase 2.
     */
    public List<RepositoryData> enrichAndFilter(List<RepositoryData> repos) {
        List<RepositoryData> enrichedList = new ArrayList<>();
        int total = repos.size();
        int processed = 0;

        System.out.println("\n========================================");
        System.out.println("  FASE 2: ENRIQUECIMIENTO CON GRAPHQL");
        System.out.println("  Repositorios a procesar: " + total);
        System.out.println("========================================");

        // Iterar de forma secuencial sobre cada repositorio obtenido en la Fase 1
        for (RepositoryData repo : repos) {
            processed++;
            System.out.println("\n[" + processed + "/" + total + "] " + repo.getFullName());

            // 1. Ir a GitHub a traer los metadatos avanzados del proyecto actual
            RepositoryData enriched = fetchMetadata(repo); 

            // 2. Si se obtuvieron datos con éxito, aplicar las reglas del validador de Fase 2
            if (enriched != null && validator.validatePhase2(enriched, filters)) { 
                enrichedList.add(enriched); // Si pasa el filtro, se agrega a la lista de aprobados
            }

            // Pausa controlada de 200ms para respetar los límites de velocidad (Rate Limit) de GitHub
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        // reporte por topic
        // === CONTEO RÁPIDO PARA EL REPORTE DE FASE 2 ===
        int micronautCount = 0;
        int springBootCount = 0;

        for (RepositoryData repo : enrichedList) {
            if (repo.getTopics() != null) {
                // Convertimos a minúsculas para asegurar que coincida con "micronaut" y "spring-boot"
                for (String t : repo.getTopics()) {
                    String topicLower = t.toLowerCase();
                    if (topicLower.contains("micronaut")) {
                        micronautCount++;
                        break; // Evita contar doble si tiene sub-topics
                    } else if (topicLower.contains("spring-boot")) {
                        springBootCount++;
                        break; 
                    }
                }
            }
        }

        System.out.println("\n  === REPORTE ADICIONAL FASE 2 ===");
        System.out.println("  Repositorios sobrevivientes de Micronaut:   " + micronautCount);
        System.out.println("  Repositorios sobrevivientes de Spring Boot: " + springBootCount);
        System.out.println("========================================\n");

        // Imprimir el resumen estadístico de cuántos repositorios pasaron o se descartaron
        validator.printPhase2Report();
        return enrichedList;
    }

    /**
     * Construye la consulta estructurada de GraphQL utilizando variables, configura las cabeceras
     * HTTP POST de autenticación y realiza la llamada síncrona a la infraestructura de GitHub.
     * @param repo Objeto de datos del repositorio actual.
     * @return El mismo objeto RepositoryData enriquecido, o null si ocurrió un fallo de red.
     */
    private RepositoryData fetchMetadata(RepositoryData repo) {
        try {
            // Dividir el nombre completo (ej. "owner/repo-name") para extraer los parámetros requeridos
            String[] parts = repo.getFullName().split("/");
            String owner = parts[0];
            String name  = parts[1];

            // Definición del Query de GraphQL usando sintaxis oficial de GitHub v4
            String graphqlQuery =
                "query($owner: String!, $name: String!) {" +
                "  repository(owner: $owner, name: $name) {" +
                "    defaultBranchRef {" +
                "      name " +
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

            // Definición de las variables dinámicas del query para evitar inyecciones o errores de string
            JsonObject variables = new JsonObject();
            variables.addProperty("owner", owner);
            variables.addProperty("name", name);

            // Construcción del cuerpo del payload JSON final (query + variables)
            JsonObject body = new JsonObject();
            body.addProperty("query", graphqlQuery);
            body.add("variables", variables);

            // Instanciar el cliente HTTP nativo de Java para procesar la petición POST
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GRAPHQL_URL))
                .header("Authorization", "Bearer " + token) // Inyección del token de seguridad
                .header("Content-Type", "application/json") // Declarar cuerpo en formato JSON
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

            // Realizar el envío de la petición de manera síncrona
            HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

            // Validar que el servidor de GitHub responda con el código HTTP 200 OK
            if (response.statusCode() != 200) {
                System.err.println("  [ERROR HTTP " + response.statusCode() + "]");
                return null;
            }

            // Enviar la cadena JSON recibida al método encargado del desglose (Parsing)
            return parseResponse(response.body(), repo);

        } catch (Exception e) {
            System.err.println("  [ERROR en petición] " + e.getMessage());
            return null;
        }
    }

    /**
     * Desglosa de manera segura el árbol de objetos JSON devuelto por la API GraphQL de GitHub.
     * Controla la existencia de valores nulos o campos deshabilitados para evitar NullPointerExceptions.
     * @param responseBody Cadena JSON sin procesar devuelta por el servidor.
     * @param repo Objeto de datos en memoria donde se inyectará la información extraída.
     * @return El objeto RepositoryData actualizado con los nuevos metadatos.
     */
    private RepositoryData parseResponse(String responseBody, RepositoryData repo) {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);

            // 1. Validar si GitHub reportó algún error de sintaxis, esquema o permisos en la petición
            if (json.has("errors")) {
                System.err.println("  [GraphQL Error] " + json.get("errors"));
                return null;
            }

            // 2. Extraer de forma directa el nodo principal de datos del repositorio
            JsonObject repoData = json
                .getAsJsonObject("data")
                .getAsJsonObject("repository");

            // Validar si el proyecto es privado o dejó de existir repentinamente (Error 404 simulado)
            if (repoData == null || repoData.isJsonNull()) {
                System.err.println("  [Error] Repositorio no encontrado en GraphQL");
                return null;
            }

            // 3. Extracción segura de la rama por defecto y del total de commits (Fase 3 v2)
            int commitCount = 0;
            JsonElement branchRefEl = repoData.get("defaultBranchRef");
            if (branchRefEl != null && !branchRefEl.isJsonNull()) {
                JsonObject branchRefObj = branchRefEl.getAsJsonObject();
                
                // Extraer el nombre de la rama para la Fase 3
                if (branchRefObj.has("name")) {
                    repo.setDefaultBranch(branchRefObj.get("name").getAsString());
                }
                
                // Extraer el total de commits del historial 
                JsonElement target = branchRefObj.get("target");
                if (target != null && !target.isJsonNull()) {
                    JsonElement history = target.getAsJsonObject().get("history");
                    if (history != null && !history.isJsonNull()) {
                        commitCount = history.getAsJsonObject().get("totalCount").getAsInt();
                    }
                }
            }

            // 4. Extracción del nombre comercial de la licencia de software (ej. "MIT License")
            String license = "No license";
            JsonElement licenseInfo = repoData.get("licenseInfo");
            if (licenseInfo != null && !licenseInfo.isJsonNull()) {
                license = licenseInfo.getAsJsonObject().get("name").getAsString();
            }

            // 5. Extracción masiva en formato de lista de las etiquetas (Topics) configuradas en GitHub
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

            // 6. Extracción del número total de Watchers (Seguidores atentos del proyecto)
            int watchersCount = 0;
            JsonElement watchers = repoData.get("watchers");
            if (watchers != null && !watchers.isJsonNull()) {
                watchersCount = watchers.getAsJsonObject().get("totalCount").getAsInt();
            }

            // 7. Extracción del estado del módulo de control de anomalías (Issues Habilitados)
            boolean hasIssuesEnabled = false;
            JsonElement issuesEl = repoData.get("hasIssuesEnabled");
            if (issuesEl != null && !issuesEl.isJsonNull()) {
                hasIssuesEnabled = issuesEl.getAsBoolean();
            }

            // 8. Seteo y enriquecimiento definitivo del objeto RepositoryData original
            repo.setCommitCount(commitCount);
            repo.setLicense(license);
            repo.setTopics(topics);
            repo.setWatchersCount(watchersCount);
            repo.setHasIssuesEnabled(hasIssuesEnabled);

            // Mostrar el resumen visual por consola de lo que se acaba de guardar
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