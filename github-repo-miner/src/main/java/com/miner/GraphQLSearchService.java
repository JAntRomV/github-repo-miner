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
    
    // URL única y centralizada para todas las operaciones de la API GraphQL v4 de GitHub
    private static final String GRAPHQL_URL = "https://api.github.com/graphql";

    // Atributos de configuración, filtros y utilerías
    private final String token;             // Almacenará la credencial de acceso a GitHub
    private final SearchFilters filters;     // Criterios o umbrales de filtrado (ej. commits mínimos)
    private final SecondValidator validator; // Componente encargado de auditar y contar los aprobados/descartados
    private final Gson gson;                 // Herramienta de Google para convertir objetos a JSON y viceversa

    /**
     * Constructor de la clase: Inicializa los componentes y recupera el Token de seguridad.
     */
    public GraphQLSearchService(SearchFilters filters, SecondValidator validator) {
        // Práctica de seguridad recomendada: No dejar el token escrito directo en el código (hardcoded),
        // sino leerlo desde las variables de entorno del sistema operativo.
        this.token     = System.getenv("GITHUB_TOKEN");
        this.filters   = filters;
        this.validator = validator;
        this.gson      = new Gson();
    }

    /**
     * MÁSTER ORQUESTADOR: Toma los repositorios de la Fase REST, extrae nuevos datos en bucle,
     * valida las condiciones y genera la lista enriquecida definitiva.
     */
    public List<EnrichedRepositoryData> enrichAndFilter(List<RepositoryData> repos) {
        // Lista donde guardaremos únicamente los repositorios que pasen la segunda validación
        List<EnrichedRepositoryData> enrichedList = new ArrayList<>();
        int total     = repos.size();
        int processed = 0;

        // Banner informativo en la consola para monitorear la ejecución en tiempo real
        System.out.println("\n========================================");
        System.out.println("  FASE 2: ENRIQUECIMIENTO CON GRAPHQL");
        System.out.println("  Repositorios a procesar: " + total);
        System.out.println("========================================");

        // Iterar en bucle sobre cada repositorio candidato obtenido en el Issue #1
        for (RepositoryData repo : repos) {
            processed++;
            System.out.println("\n[" + processed + "/" + total + "] " + repo.getFullName());

            // 1. Ir a buscar los metadatos extendidos a GitHub (Red)
            EnrichedRepositoryData enriched = fetchMetadata(repo);

            // 2. Si la petición fue exitosa y además cumple las reglas del Segundo Validador (ej. commits >= 10)
            if (enriched != null && validator.validate(enriched, filters)) {
                enrichedList.add(enriched); // Se agrega oficialmente al set de datos final
            }

            // Pausa técnica obligatoria (Politeness delay): Duerme el programa 200 milisegundos
            // para no saturar los Rate Limits de GitHub y evitar baneos temporales por abuso de red.
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        // Imprime en consola el resumen estadístico acumulado del embudo de descarte de la Fase 2
        validator.printReport();
        return enrichedList;
    }

    /**
     * MOTOR DE RED: Construye el cuerpo de la query GraphQL, ejecuta la solicitud HTTP POST
     * y controla errores a nivel de transporte o servidores.
     */
    private EnrichedRepositoryData fetchMetadata(RepositoryData repo) {
        try {
            // Divide "owner/repo-name" usando la diagonal para extraer los dos datos requeridos por la Query
            String[] parts = repo.getFullName().split("/");
            String owner = parts[0];
            String name  = parts[1];

            // Diseña la Query exacta que queremos. GraphQL nos permite pedir exactamente lo que requerimos,
            // evitando el Over-fetching (descargar datos inútiles).
            String graphqlQuery =
                "query($owner: String!, $name: String!) {" +
                "  repository(owner: $owner, name: $name) {" +
                "    defaultBranchRef {" +
                "      target {" +
                "        ... on Commit {" +
                "          history { totalCount }" +  // <-- Conteo exacto e histórico de commits de la rama principal
                "        }" +
                "      }" +
                "    }" +
                "    licenseInfo { name spdxId }" +  // Nombre legal y formato SPDX de la licencia abierta
                "    repositoryTopics(first: 10) {" +
                "      nodes { topic { name } }" +    // Etiquetas/Tópicos asignados al repositorio (máximo 10)
                "    }" +
                "    watchers { totalCount }" +       // Conteo de observadores del proyecto
                "    hasIssuesEnabled" +              // Bandera booleana (si tiene el módulo de Issues activado)
                "  }" +
                "}";

            // Se crean las variables del JSON dinámico que acompañará la Query
            JsonObject variables = new JsonObject();
            variables.addProperty("owner", owner);
            variables.addProperty("name", name);

            // Se unifica el cuerpo del Payload del request siguiendo la especificación estándar de GraphQL
            JsonObject body = new JsonObject();
            body.addProperty("query", graphqlQuery);
            body.add("variables", variables);

            // Uso del cliente HTTP nativo de Java (disponible a partir de Java 11)
            HttpClient client = HttpClient.newHttpClient();
            
            // Construcción del paquete Request inyectando métodos, URIs y los headers necesarios
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GRAPHQL_URL))
                .header("Authorization", "Bearer " + token) // Autorización segura por token
                .header("Content-Type", "application/json")   // Especifica que enviamos texto en formato JSON
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body))) // Transforma el objeto JsonObject a un String plano
                .build();

            // Sincroniza la llamada de red y almacena la respuesta cruda en formato de texto plano (String)
            HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

            // Si el servidor de GitHub responde algo diferente al código 200 OK, algo salió mal
            if (response.statusCode() != 200) {
                System.err.println("  [ERROR HTTP " + response.statusCode() + "]");
                return null;
            }

            // Pasa el cuerpo del JSON de respuesta al método extractor
            return parseResponse(response.body(), repo);

        } catch (Exception e) {
            System.err.println("  [ERROR en petición] " + e.getMessage());
            return null;
        }
    }

    /**
     * EXTRACTOR DE DATOS (Mapeador): Navega el árbol JSON jerárquico que envía GitHub,
     * extrae con precaución cada metadato y consolida el objeto de salida unificado.
     */
    private EnrichedRepositoryData parseResponse(String responseBody, RepositoryData repo) {
        try {
            // Convierte el texto de la respuesta en un árbol de objetos JSON manipulable por Java
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);

            // Control de excepciones semánticas de GraphQL (ejemplo: Query mal estructurada o problemas de permisos)
            if (json.has("errors")) {
                System.err.println("  [GraphQL Error] " + json.get("errors"));
                return null;
            }

            // Acceder al nodo raíz de la respuesta de datos: json.data.repository
            JsonObject repoData = json
                .getAsJsonObject("data")
                .getAsJsonObject("repository");

            // Si es nulo, significa que el repositorio ya no existe o cambió de nombre/visibilidad
            if (repoData == null || repoData.isJsonNull()) {
                System.err.println("  [Error] Repositorio no encontrado en GraphQL");
                return null;
            }

            // --- EXTRACCIÓN JALÓN POR JALÓN CON VALIDACIONES ANTE NULOS (Defensive Programming) ---

            // Conteo de commits: Se navega con cuidado por defaultBranchRef -> target -> history -> totalCount
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

            // Licencia: Verifica si el proyecto tiene un bloque de licencia legal asignado
            String license = "No license";
            JsonElement licenseInfo = repoData.get("licenseInfo");
            if (licenseInfo != null && !licenseInfo.isJsonNull()) {
                license = licenseInfo.getAsJsonObject().get("name").getAsString();
            }

            // Tópicos/Etiquetas: Recorre el arreglo JSON dinámico "nodes" y extrae el String del nombre de cada tópico
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

            // Watchers: Conteo de observadores registrados
            int watchersCount = 0;
            JsonElement watchers = repoData.get("watchers");
            if (watchers != null && !watchers.isJsonNull()) {
                watchersCount = watchers.getAsJsonObject().get("totalCount").getAsInt();
            }

            // Habilitación de Issues: Bandera lógica útil para auditar el mantenimiento comunitario del proyecto
            boolean hasIssuesEnabled = false;
            JsonElement issuesEl = repoData.get("hasIssuesEnabled");
            if (issuesEl != null && !issuesEl.isJsonNull()) {
                hasIssuesEnabled = issuesEl.getAsBoolean();
            }

            // --- CONSTRUCCIÓN DE LA ENTIDAD DE SALIDA FINAL ---
            EnrichedRepositoryData enriched = new EnrichedRepositoryData();
            
            // Se realiza la transferencia/copiado de los datos que heredamos de la Fase 1 (REST)
            enriched.setFullName(repo.getFullName());
            enriched.setDescription(repo.getDescription());
            enriched.setHtmlUrl(repo.getHtmlUrl());
            enriched.setStars(repo.getStars());
            enriched.setSize(repo.getSize());
            enriched.setLanguage(repo.getLanguage());
            enriched.setPushedAt(repo.getPushedAt());
            enriched.setForks(repo.getForks());
            enriched.setOpenIssues(repo.getOpenIssues());
            
            // Se le inyectan los nuevos superpoderes o metadatos extendidos obtenidos en esta Fase 2 (GraphQL)
            enriched.setCommitCount(commitCount);
            enriched.setLicense(license);
            enriched.setTopics(topics);
            enriched.setWatchersCount(watchersCount);
            enriched.setHasIssuesEnabled(hasIssuesEnabled);

            // Print de auditoría inmediata por consola para monitorear el éxito del parseo
            System.out.println("  Metadatos: commits=" + commitCount
                + " | licencia=" + license
                + " | watchers=" + watchersCount
                + " | topics=" + topics);

            return enriched; // Devuelve el objeto unificado completo

        } catch (Exception e) {
            System.err.println("  [ERROR al parsear respuesta] " + e.getMessage());
            return null;
        }
    }
}