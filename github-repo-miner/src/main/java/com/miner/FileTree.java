package com.miner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Envuelve la respuesta de la Git Trees API (recursive=1).
 * Resuelve existencia de archivos y directorios SIN descargar contenido —
 * exactamente como pide la sección "Adquisición de archivos sin clonar".
 */
public class FileTree {

    private final List<String> paths = new ArrayList<>();
    private boolean truncated = false;

    public static FileTree fromJson(JsonObject treeResponse) {
        FileTree ft = new FileTree();

        if (treeResponse.has("truncated")) {
            ft.truncated = treeResponse.get("truncated").getAsBoolean();
        }

        JsonArray items = treeResponse.getAsJsonArray("tree");
        for (JsonElement el : items) {
            JsonObject obj = el.getAsJsonObject();
            // Solo nos interesan los blobs (archivos); "tree" son directorios
            if (obj.get("type").getAsString().equals("blob")) {
                ft.paths.add(obj.get("path").getAsString());
            }
        }
        return ft;
    }

    // Constructor de conveniencia para pruebas unitarias
    public static FileTree fromPaths(List<String> paths) {
        FileTree ft = new FileTree();
        ft.paths.addAll(paths);
        return ft;
    }

    // ¿Existe este archivo exacto? (ej: "pom.xml", ".travis.yml", "CITATION.cff")
    public boolean fileExists(String exactPath) {
        return paths.contains(exactPath);
    }

    // ¿Existe algún archivo bajo este directorio? Cubre proyectos raíz Y multi-módulo
    // (ej: "src/test/java" o "moduloA/src/test/java")
    public boolean directoryExists(String dirPrefix) {
        String prefixSlash = dirPrefix + "/";
        String nestedSlash = "/" + dirPrefix + "/";
        return paths.stream().anyMatch(p -> p.startsWith(prefixSlash) || p.contains(nestedSlash));
    }

    // ¿Algún path contiene este segmento en cualquier posición?
    // (ej: META-INF/native-image/ puede estar anidado en src/main/resources/...)
    public boolean pathContains(String segment) {
        return paths.stream().anyMatch(p -> p.contains(segment));
    }

    // Cuenta archivos que matcheen un patrón (ej: *Test.java bajo src/test)
    public int countMatching(Pattern pattern) {
        return (int) paths.stream().filter(p -> pattern.matcher(p).find()).count();
    }

    // Detecta el build tool mirando SOLO el árbol — sin descargar nada
    public BuildTool detectBuildTool() {
        if (fileExists("pom.xml")) return BuildTool.MAVEN;
        if (fileExists("build.gradle") || fileExists("build.gradle.kts")) return BuildTool.GRADLE;
        return BuildTool.NONE;
    }

    // Nombre del build file principal a descargar
    public String getPrimaryBuildFilePath() {
        if (fileExists("pom.xml")) return "pom.xml";
        if (fileExists("build.gradle.kts")) return "build.gradle.kts";
        if (fileExists("build.gradle")) return "build.gradle";
        return null;
    }

    public boolean isTruncated() { return truncated; }
    public List<String> getPaths() { return paths; }
}