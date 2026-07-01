package com.miner;

public class RepositoryParser {

    // Palabras clave que identifican Spring Boot en un build file
    private static final String[] SPRING_BOOT_MARKERS = {
        "org.springframework.boot",
        "spring-boot-starter",
        "spring-boot-autoconfigure",
        "SpringApplication",
        "spring-boot"
    };

    // Palabras clave que identifican Micronaut en un build file
    private static final String[] MICRONAUT_MARKERS = {
        "io.micronaut",
        "micronaut-core",
        "micronaut-http",
        "micronaut-runtime",
        "io.micronaut.application"
    };

    // Parsea pom.xml buscando dependencias de Spring Boot o Micronaut
    // Usamos búsqueda de texto en lugar de DOM para mayor robustez
    public ParseResult parsePomXml(String content) {
        ParseResult result = new ParseResult();
        result.setBuildTool("Maven");
        searchMarkers(content, result);
        return result;
    }

    // Parsea build.gradle o build.gradle.kts (Gradle con Groovy o Kotlin DSL)
    public ParseResult parseBuildGradle(String content) {
        ParseResult result = new ParseResult();
        result.setBuildTool("Gradle");
        searchMarkers(content, result);
        return result;
    }

    // Busca los marcadores de framework en el contenido del archivo
    private void searchMarkers(String content, ParseResult result) {
        String contentLower = content.toLowerCase();

        for (String marker : SPRING_BOOT_MARKERS) {
            if (contentLower.contains(marker.toLowerCase())) {
                result.setHasSpringBoot(true);
                result.getDetectedDeps().add("spring-boot → " + marker);
                break; // con uno es suficiente
            }
        }

        for (String marker : MICRONAUT_MARKERS) {
            if (contentLower.contains(marker.toLowerCase())) {
                result.setHasMicronaut(true);
                result.getDetectedDeps().add("micronaut → " + marker);
                break;
            }
        }
    }
}