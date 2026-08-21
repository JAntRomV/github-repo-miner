package com.miner;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidateWithRealRepo {

    private final String token = System.getenv("GITHUB_TOKEN");
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String API_BASE = "https://api.github.com/repos";

    // Patrón para contar métodos — busca método + paréntesis + llaves
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "(?:public|private|protected)?\\s+(?:static)?\\s+(?:synchronized)?\\s*" +
        "(?:\\w+[\\[\\]]*\\s+)+\\w+\\s*\\(.*?\\)\\s*(?:throws\\s+[\\w,\\s]+)?\\s*\\{",
        Pattern.MULTILINE | Pattern.DOTALL
    );

  public static void main(String[] args) throws Exception {
        // ─────────────────────────────────────────────────────────────────
        // CONFIGURACIÓN DE REPOSITORIO DE PRUEBA (HARDCODEO)
        // Deja activa (sin //) únicamente la línea del repositorio que quieres evaluar:
        // ─────────────────────────────────────────────────────────────────
        
        String fullName = "";
        //fullName = "kokuwaio/micronaut-openapi-codegen";
        // * fullName = "newbee-ltd/newbee-mall";
        //fullName = "zfile-dev/zfile"; // <- Actualmente seleccionado
        // *fullName = "geekidea/spring-boot-plus";
        // *fullName = "atjiu/pybbs";
        // *fullName = "YeautyYE/netty-websocket-spring-boot-starter";
        //*fullName = "LianjiaTech/retrofit-spring-boot-starter";
        //*fullName = "murraco/spring-boot-jwt";
        // *fullName = "adorsys/keycloak-config-cli";
        // * fullName = "conductor-oss/conductor"; // esta roto 
        fullName = "adorsys/keycloak-config-cli";

        // Separamos el dueño y el nombre del repositorio usando la diagonal
        String[] parts = fullName.split("/");
        String owner = parts[0];
        String repo = parts[1];

        /* Comentamos el uso original por argumentos de terminal
        if (args.length < 2) {
            System.out.println("❌ Uso: java ValidateWithRealRepo <owner> <repo>");
            System.out.println("   Ejemplo: java ValidateWithRealRepo spring-projects spring-boot");
            System.exit(1);
        }

        String owner = args[0];
        String repo = args[1];
        */

        ValidateWithRealRepo validator = new ValidateWithRealRepo();
        validator.validateRepo(owner, repo);
    }

    public void validateRepo(String owner, String repo) throws Exception {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║  VALIDACIÓN REAL DE HEURÍSTICA");
        System.out.println("║  Repo: " + owner + "/" + repo);
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

        if (token == null || token.isEmpty()) {
            System.err.println("❌ ERROR: GITHUB_TOKEN no configurado.");
            System.err.println("Ejecuta: export GITHUB_TOKEN=tu_token_aqui");
            System.exit(1);
        }

        // Paso 1: Obtener tamaño total estimado
        System.out.println("[ 1/3 ] Obteniendo árbol de archivos...");
        TreeMetrics metrics = fetchTreeMetrics(owner, repo);
        int estimatedMethods = (metrics.totalBytes / 1024) * 6;

        System.out.printf(
            "        ✓ %,d bytes totales | %d archivos .java | Estimado: %d métodos\n\n",
            metrics.totalBytes, metrics.javaFileCount, estimatedMethods
        );

        // Paso 2: Descargar los N archivos .java más grandes
        System.out.println("[ 2/3 ] Descargando los 10 archivos .java más grandes...");
        List<JavaFileData> topFiles = fetchTopJavaFiles(owner, repo, 10);

        System.out.printf("        ✓ Descargados %d archivos\n\n", topFiles.size());

        // Paso 3: Contar métodos REALES con regex
        System.out.println("[ 3/3 ] Contando métodos en los archivos descargados...\n");

        int totalRealMethods = 0;

        System.out.println("Archivo\t\t\t\t\tTamaño\t\tMétodos\t\tPromedio");
        System.out.println("─".repeat(100));

        for (JavaFileData file : topFiles) {
            int methodsInFile = countMethodsInCode(file.content);
            totalRealMethods += methodsInFile;

            String fileName = file.path.length() > 40
                ? "..." + file.path.substring(file.path.length() - 37)
                : file.path;

            int avgMethodsPerKb = file.size > 0 ? (methodsInFile * 1024) / file.size : 0;

            System.out.printf(
                "%s\t%,d\t\t%d\t\t%d/KB\n",
                fileName, file.size, methodsInFile, avgMethodsPerKb
            );
        }

        System.out.println("─".repeat(100));
        System.out.printf("\nTotal métodos en muestra: %d\n", totalRealMethods);
        System.out.printf("Promedio por archivo: %.1f métodos\n", (double) totalRealMethods / topFiles.size());

        // Paso 4: Comparación
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    COMPARACIÓN FINAL                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

        int extrapolatedTotal = totalRealMethods * (metrics.javaFileCount / topFiles.size());

        System.out.printf("Estimado (heurística tamaño):   %,d métodos\n", estimatedMethods);
        System.out.printf("Real en muestra (conteo):       %d métodos\n", totalRealMethods);
        System.out.printf("Extrapolado a todo repo:        %,d métodos\n\n", extrapolatedTotal);

        double error = Math.abs(estimatedMethods - extrapolatedTotal) * 100.0 / extrapolatedTotal;
        System.out.printf("ERROR DE ESTIMACIÓN: %.1f%%\n\n", error);

        if (error < 20) {
            System.out.println("✅ HEURÍSTICA VÁLIDA — Error < 20%");
        } else if (error < 40) {
            System.out.println("⚠ HEURÍSTICA ACEPTABLE — Error < 40%");
        } else {
            System.out.println("❌ HEURÍSTICA INEXACTA — Error > 40%");
        }

        // Sugerencia de ajuste
        double ratio = (double) extrapolatedTotal / estimatedMethods;
        int newConstant = Math.round((float) ratio * 3);
        System.out.printf("\n💡 Sugerencia: usar (totalBytes / 1024) * %d en lugar de * 3\n\n", newConstant);
    }

    // ═══════════════════════════════════════════════════════════════
    // MÉTODO 1: Obtener tamaño total del repo
    // ═══════════════════════════════════════════════════════════════
    public TreeMetrics fetchTreeMetrics(String owner, String repo) throws Exception {
        String url = String.format(
            "https://api.github.com/repos/%s/%s/git/trees/HEAD?recursive=1",
            owner, repo
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github.v3+json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("ERROR HTTP " + response.statusCode() + " al obtener árbol");
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        JsonArray tree = json.getAsJsonArray("tree");

        int totalBytes = 0;
        int javaFileCount = 0;

        for (JsonElement item : tree) {
            JsonObject obj = item.getAsJsonObject();
            String path = obj.get("path").getAsString();

            if (path.endsWith(".java")) {
                int size = obj.get("size").getAsInt();
                totalBytes += size;
                javaFileCount++;
            }
        }

        return new TreeMetrics(owner, repo, totalBytes, javaFileCount);
    }

    // ═══════════════════════════════════════════════════════════════
    // MÉTODO 2: Descargar los 10 archivos más grandes
    // ═══════════════════════════════════════════════════════════════
    public List<JavaFileData> fetchTopJavaFiles(String owner, String repo, int topN) throws Exception {
        String url = String.format(
            "https://api.github.com/repos/%s/%s/git/trees/HEAD?recursive=1",
            owner, repo
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        JsonArray tree = json.getAsJsonArray("tree");

        // Ordenar archivos .java por tamaño (mayor primero)
        List<JavaFileInfo> javaFiles = new ArrayList<>();
        for (JsonElement item : tree) {
            JsonObject obj = item.getAsJsonObject();
            String path = obj.get("path").getAsString();
            if (path.endsWith(".java")) {
                javaFiles.add(new JavaFileInfo(path, obj.get("size").getAsInt()));
            }
        }

        javaFiles.sort((a, b) -> Integer.compare(b.size, a.size)); // Mayor primero

        // Tomar los top N
        List<JavaFileData> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, javaFiles.size()); i++) {
            JavaFileInfo info = javaFiles.get(i);
            String content = downloadFile(owner, repo, info.path);
            if (content != null) {
                result.add(new JavaFileData(info.path, info.size, content));
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // MÉTODO 3: Descargar un archivo vía Contents API
    // ═══════════════════════════════════════════════════════════════
    public String downloadFile(String owner, String repo, String path) throws Exception {
        String url = String.format(
            "https://api.github.com/repos/%s/%s/contents/%s",
            owner, repo, path
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return null;
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        String base64Content = json.get("content").getAsString();
        String base64Clean = base64Content.replace("\n", "").replace("\\n", "");

        byte[] decoded = Base64.getDecoder().decode(base64Clean);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    // ═══════════════════════════════════════════════════════════════
    // MÉTODO 4: Contar métodos con REGEX
    // ═══════════════════════════════════════════════════════════════
    public int countMethodsInCode(String javaCode) {
        Matcher matcher = METHOD_PATTERN.matcher(javaCode);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════════
    // CLASES AUXILIARES
    // ═══════════════════════════════════════════════════════════════
    static class TreeMetrics {
        String owner;
        String repo;
        int totalBytes;
        int javaFileCount;

        TreeMetrics(String owner, String repo, int totalBytes, int javaFileCount) {
            this.owner = owner;
            this.repo = repo;
            this.totalBytes = totalBytes;
            this.javaFileCount = javaFileCount;
        }
    }

    static class JavaFileInfo {
        String path;
        int size;

        JavaFileInfo(String path, int size) {
            this.path = path;
            this.size = size;
        }
    }

    static class JavaFileData {
        String path;
        int size;
        String content;

        JavaFileData(String path, int size, String content) {
            this.path = path;
            this.size = size;
            this.content = content;
        }
    }
}