package com.miner;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Analiza build.gradle / build.gradle.kts con búsqueda de texto (regex).
 * Gradle es código ejecutable, no datos — un parser real requeriría la
 * Gradle Tooling API (clonar y ejecutar Gradle). Para un escaneo ligero
 * se usa análisis estático de texto, cruzando también libs.versions.toml
 * y gradle.properties cuando existen.
 *
 * LIMITACIÓN CONOCIDA (documentada por el tesista): el texto plano no
 * resuelve TODAS las versiones si vienen de un catálogo externo complejo.
 * El camino riguroso (Tooling API) se reserva para finalistas.
 */
public class GradleAnalyzer implements BuildFileAnalyzer {

    private static final Pattern TEST_FILE_PATTERN =
        Pattern.compile("^src/test/.*Test\\.(java|kt)$");

    @Override
    public boolean supports(BuildTool tool) {
        return tool == BuildTool.GRADLE;
    }

    @Override
    public TechProfile analyze(FileTree tree, Map<String, String> buildFiles) {
        String buildContent = buildFiles.getOrDefault("build.gradle",
                               buildFiles.getOrDefault("build.gradle.kts", ""));

        if (buildContent.isEmpty()) {
            return TechProfile.empty(BuildTool.GRADLE);
        }

        // Cruza con version catalog y gradle.properties si se descargaron
        String versionsToml = buildFiles.getOrDefault("gradle/libs.versions.toml", "");
        String gradleProps  = buildFiles.getOrDefault("gradle.properties", "");
        String combined = buildContent + "\n" + versionsToml + "\n" + gradleProps;

        boolean isMicronaut = contains(combined, "io.micronaut.application")
            || contains(combined, "io.micronaut:");
        boolean isSpringBoot = contains(combined, "org.springframework.boot");

        Framework framework = isMicronaut && isSpringBoot ? Framework.BOTH
            : isMicronaut ? Framework.MICRONAUT
            : isSpringBoot ? Framework.SPRING_BOOT
            : Framework.NONE;

        boolean java21 = contains(combined, "JavaLanguageVersion.of(21)")
            || contains(combined, "VERSION_21");
        int javaVersion = java21 ? 21 : 0;

        boolean graalvmReady = contains(combined, "org.graalvm.buildtools.native")
            || tree.pathContains("META-INF/native-image")
            || framework == Framework.MICRONAUT;

        boolean hasTestDir = tree.directoryExists("src/test/java")
            || tree.directoryExists("src/test/kotlin");
        TestFramework testFramework = detectTestFramework(combined);
        boolean hasTestSuite = hasTestDir && testFramework != TestFramework.NONE;
        int testFileCount = tree.countMatching(TEST_FILE_PATTERN);

        boolean jmhPresent = tree.directoryExists("src/jmh")
            || contains(combined, "me.champeau.jmh")
            || contains(combined, "org.openjdk.jmh:jmh-core");
        boolean jmhCandidate = jmhPresent || looksLikeJmhCandidate(tree);

        boolean profilingCandidate = tree.getPaths().stream()
                .anyMatch(p -> p.endsWith("Application.java") || p.endsWith("Application.kt"))
            || contains(combined, "micrometer");

        Sector sector = tree.fileExists("CITATION.cff") ? Sector.ACADEMIC : Sector.UNKNOWN;
        boolean travisCi = tree.fileExists(".travis.yml");

        boolean passesHardFilters = framework != Framework.NONE && java21 && hasTestSuite;

        return new TechProfile(
            BuildTool.GRADLE, framework, javaVersion, java21, graalvmReady,
            hasTestSuite, testFramework, testFileCount,
            jmhPresent, jmhCandidate, profilingCandidate,
            sector, travisCi, passesHardFilters
        );
    }

    private boolean contains(String haystack, String needle) {
        return haystack.toLowerCase().contains(needle.toLowerCase());
    }

    private TestFramework detectTestFramework(String content) {
        if (contains(content, "micronaut-test-junit5")) return TestFramework.MICRONAUT_TEST_JUNIT5;
        if (contains(content, "spring-boot-starter-test")) return TestFramework.SPRING_BOOT_STARTER_TEST;
        if (contains(content, "junit-jupiter")) return TestFramework.JUNIT_JUPITER;
        return TestFramework.NONE;
    }

    private boolean looksLikeJmhCandidate(FileTree tree) {
        return tree.getPaths().stream().anyMatch(p ->
            p.contains("codec") || p.contains("parser") ||
            p.contains("serializ") || p.contains("algorithm"));
    }
}