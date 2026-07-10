package com.miner;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

/**
 * Descarga archivos crudos desde raw.githubusercontent.com.
 * A diferencia de la Contents API (usada en la versión anterior de Fase 3),
 * este CDN devuelve el contenido tal cual — NO hay que decodificar Base64.
 */
public class RawFileFetcher {

    private static final String RAW_BASE = "https://raw.githubusercontent.com";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public Optional<String> fetch(String owner, String repo, String branch, String path) {
        String url = String.format("%s/%s/%s/%s/%s", RAW_BASE, owner, repo, branch, path);

        // Reintentos: 3 intentos, 1s de espera — equivalente al @Retryable del documento
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "repo-evaluator")
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return Optional.of(response.body());
                }
                if (response.statusCode() == 404) {
                    return Optional.empty(); // archivo no existe — no reintentar
                }
                // otros códigos (403, 5xx) → reintentar
            } catch (Exception e) {
                // reintentar
            }
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        return Optional.empty();
    }
}