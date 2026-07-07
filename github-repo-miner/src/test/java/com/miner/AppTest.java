package com.miner;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class AppTest {

    // ═══════════════════════════════════════════
    // Tests SearchFilters (sin cambios)
    // ═══════════════════════════════════════════

    @Test
    public void testLanguagesContainRequiredTargets() {
        SearchFilters filters = new SearchFilters();
        List<String> langs = filters.getLanguages();
        assertTrue("Debe contener Java", langs.contains("Java"));
        assertTrue("Debe contener Python", langs.contains("Python"));
        assertTrue("Debe contener JavaScript", langs.contains("JavaScript"));
        assertEquals("Solo deben existir 3 lenguajes", 3, langs.size());
    }

    @Test
    public void testMinCommitsIsPositive() {
        SearchFilters filters = new SearchFilters();
        assertTrue("minCommits debe ser mayor a 0", filters.getMinCommits() > 0);
    }

    @Test
    public void testTechnicalFrameworksArePresent() {
        SearchFilters filters = new SearchFilters();
        List<String> fw = filters.getTechnicalFrameworks();
        assertFalse("No debe estar vacía", fw.isEmpty());
        assertTrue("Debe incluir spring-boot", fw.contains("spring-boot"));
        assertTrue("Debe incluir micronaut", fw.contains("micronaut"));
    }

    // ═══════════════════════════════════════════
    // Tests RepositoryValidator — Fase 2 (sin cambios)
    // ═══════════════════════════════════════════

    @Test
    public void testValidatorApprovesValidRepo() {
        RepositoryValidator validator = new RepositoryValidator();
        SearchFilters filters = new SearchFilters();

        RepositoryData repo = new RepositoryData();
        repo.setCommitCount(100);
        repo.setLicense("MIT License");
        repo.setTopics(new ArrayList<>());

        assertTrue("Debe aprobar repo con commits y licencia",
            validator.validatePhase2(repo, filters));
    }

    @Test
    public void testValidatorRejectsInsufficientCommits() {
        RepositoryValidator validator = new RepositoryValidator();
        SearchFilters filters = new SearchFilters();

        RepositoryData repo = new RepositoryData();
        repo.setCommitCount(2);
        repo.setLicense("MIT License");
        repo.setTopics(new ArrayList<>());

        assertFalse("Debe rechazar repo con menos de 10 commits",
            validator.validatePhase2(repo, filters));
    }

    @Test
    public void testValidatorRejectsNoLicense() {
        RepositoryValidator validator = new RepositoryValidator();
        SearchFilters filters = new SearchFilters();

        RepositoryData repo = new RepositoryData();
        repo.setCommitCount(50);
        repo.setLicense("No license");
        repo.setTopics(new ArrayList<>());

        assertFalse("Debe rechazar repo sin licencia",
            validator.validatePhase2(repo, filters));
    }

    // ═══════════════════════════════════════════
    // Tests Fase 3 v2 — RepositoryValidator.validatePhase3(TechProfile)
    // ═══════════════════════════════════════════

    @Test
    public void testPhase3ApprovesValidProfile() {
        RepositoryValidator validator = new RepositoryValidator();

        TechProfile profile = new TechProfile(
            BuildTool.MAVEN, Framework.SPRING_BOOT, 21, true, true,
            true, TestFramework.SPRING_BOOT_STARTER_TEST, 5,
            false, false, true, Sector.INDUSTRY, false, true
        );

        assertTrue("Debe aprobar perfil con framework, Java21 y tests",
            validator.validatePhase3(profile));
    }

    @Test
    public void testPhase3RejectsNoFramework() {
        RepositoryValidator validator = new RepositoryValidator();

        TechProfile profile = new TechProfile(
            BuildTool.MAVEN, Framework.NONE, 21, true, false,
            true, TestFramework.JUNIT_JUPITER, 3,
            false, false, false, Sector.UNKNOWN, false, false
        );

        assertFalse("Debe rechazar perfil sin framework detectado",
            validator.validatePhase3(profile));
    }

    @Test
    public void testPhase3RejectsNoJava21() {
        RepositoryValidator validator = new RepositoryValidator();

        TechProfile profile = new TechProfile(
            BuildTool.MAVEN, Framework.SPRING_BOOT, 17, false, false,
            true, TestFramework.SPRING_BOOT_STARTER_TEST, 5,
            false, false, false, Sector.UNKNOWN, false, false
        );

        assertFalse("Debe rechazar perfil que no sea Java 21",
            validator.validatePhase3(profile));
    }

    @Test
    public void testPhase3RejectsNoTestSuite() {
        RepositoryValidator validator = new RepositoryValidator();

        TechProfile profile = new TechProfile(
            BuildTool.GRADLE, Framework.MICRONAUT, 21, true, true,
            false, TestFramework.NONE, 0,
            false, false, false, Sector.UNKNOWN, false, false
        );

        assertFalse("Debe rechazar perfil sin suite de pruebas",
            validator.validatePhase3(profile));
    }

    // ═══════════════════════════════════════════
    // Tests FileTree — inventario sin descargar contenido
    // ═══════════════════════════════════════════

    @Test
    public void testFileTreeDetectsMavenBuildTool() {
        FileTree tree = FileTree.fromPaths(List.of(
            "pom.xml", "src/main/java/App.java", "src/test/java/AppTest.java"
        ));

        assertEquals("Debe detectar Maven", BuildTool.MAVEN, tree.detectBuildTool());
    }

    @Test
    public void testFileTreeDetectsGradleBuildTool() {
        FileTree tree = FileTree.fromPaths(List.of(
            "build.gradle.kts", "src/main/java/App.java"
        ));

        assertEquals("Debe detectar Gradle", BuildTool.GRADLE, tree.detectBuildTool());
    }

    @Test
    public void testFileTreeDetectsNoBuildTool() {
        FileTree tree = FileTree.fromPaths(List.of(
            "README.md", "docs/guide.md"
        ));

        assertEquals("Sin pom.xml ni build.gradle debe ser NONE",
            BuildTool.NONE, tree.detectBuildTool());
    }

    @Test
    public void testFileTreeDirectoryExists() {
        FileTree tree = FileTree.fromPaths(List.of(
            "src/test/java/com/example/UserTest.java"
        ));

        assertTrue("Debe detectar src/test/java", tree.directoryExists("src/test/java"));
        assertFalse("No debe detectar src/jmh", tree.directoryExists("src/jmh"));
    }

    @Test
    public void testFileTreeFileExists() {
        FileTree tree = FileTree.fromPaths(List.of(
            "CITATION.cff", "pom.xml"
        ));

        assertTrue("Debe encontrar CITATION.cff", tree.fileExists("CITATION.cff"));
        assertFalse("No debe encontrar .travis.yml", tree.fileExists(".travis.yml"));
    }

    // ═══════════════════════════════════════════
    // Tests MavenAnalyzer — parseo XML real
    // ═══════════════════════════════════════════

    @Test
    public void testMavenAnalyzerDetectsSpringBoot() {
        MavenAnalyzer analyzer = new MavenAnalyzer();

        String fakePom =
            "<project>" +
            "  <parent>" +
            "    <groupId>org.springframework.boot</groupId>" +
            "    <artifactId>spring-boot-starter-parent</artifactId>" +
            "  </parent>" +
            "  <properties><java.version>21</java.version></properties>" +
            "</project>";

        FileTree tree = FileTree.fromPaths(List.of(
            "pom.xml", "src/test/java/AppTest.java"
        ));
        Map<String, String> buildFiles = new HashMap<>();
        buildFiles.put("pom.xml", fakePom);

        TechProfile profile = analyzer.analyze(tree, buildFiles);

        assertEquals("Debe detectar Spring Boot", Framework.SPRING_BOOT, profile.framework());
        assertEquals("Debe detectar Java 21", 21, profile.javaVersion());
        assertTrue("java21 debe ser true", profile.java21());
    }

    @Test
    public void testMavenAnalyzerDetectsMicronautAsGraalReady() {
        MavenAnalyzer analyzer = new MavenAnalyzer();

        String fakePom =
            "<project>" +
            "  <parent>" +
            "    <groupId>io.micronaut</groupId>" +
            "    <artifactId>micronaut-parent</artifactId>" +
            "  </parent>" +
            "  <properties><java.version>21</java.version></properties>" +
            "</project>";

        FileTree tree = FileTree.fromPaths(List.of("pom.xml"));
        Map<String, String> buildFiles = new HashMap<>();
        buildFiles.put("pom.xml", fakePom);

        TechProfile profile = analyzer.analyze(tree, buildFiles);

        assertEquals("Debe detectar Micronaut", Framework.MICRONAUT, profile.framework());
        assertTrue("Micronaut es nativo por diseño -> graalvmReady=true", profile.graalvmReady());
    }

    @Test
    public void testMavenAnalyzerReturnsEmptyWhenNoPom() {
        MavenAnalyzer analyzer = new MavenAnalyzer();
        FileTree tree = FileTree.fromPaths(List.of("README.md"));
        Map<String, String> buildFiles = new HashMap<>(); // vacío, sin pom.xml

        TechProfile profile = analyzer.analyze(tree, buildFiles);

        assertEquals("Sin pom.xml debe devolver framework NONE",
            Framework.NONE, profile.framework());
        assertFalse("passesHardFilters debe ser false", profile.passesHardFilters());
    }

    // ═══════════════════════════════════════════
    // Tests GradleAnalyzer — análisis de texto
    // ═══════════════════════════════════════════

    @Test
    public void testGradleAnalyzerDetectsMicronaut() {
        GradleAnalyzer analyzer = new GradleAnalyzer();

        String fakeGradle =
            "plugins { id 'io.micronaut.application' }\n" +
            "dependencies {\n" +
            "    implementation(\"io.micronaut:micronaut-http-server-netty\")\n" +
            "    testImplementation(\"io.micronaut.test:micronaut-test-junit5\")\n" +
            "}\n" +
            "java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }";

        FileTree tree = FileTree.fromPaths(List.of(
            "build.gradle", "src/test/java/AppTest.java"
        ));
        Map<String, String> buildFiles = new HashMap<>();
        buildFiles.put("build.gradle", fakeGradle);

        TechProfile profile = analyzer.analyze(tree, buildFiles);

        assertEquals("Debe detectar Micronaut", Framework.MICRONAUT, profile.framework());
        assertTrue("Debe detectar Java 21", profile.java21());
        assertEquals("Debe detectar test framework Micronaut",
            TestFramework.MICRONAUT_TEST_JUNIT5, profile.testFramework());
    }

    @Test
    public void testGradleAnalyzerDetectsJmhPlugin() {
        GradleAnalyzer analyzer = new GradleAnalyzer();

        String fakeGradle = "plugins { id 'me.champeau.jmh' version '0.7.2' }";

        FileTree tree = FileTree.fromPaths(List.of("build.gradle"));
        Map<String, String> buildFiles = new HashMap<>();
        buildFiles.put("build.gradle", fakeGradle);

        TechProfile profile = analyzer.analyze(tree, buildFiles);

        assertTrue("Debe detectar plugin JMH", profile.jmhPresent());
    }

    @Test
    public void testGradleAnalyzerReturnsEmptyWhenNoBuildFile() {
        GradleAnalyzer analyzer = new GradleAnalyzer();
        FileTree tree = FileTree.fromPaths(List.of("README.md"));
        Map<String, String> buildFiles = new HashMap<>();

        TechProfile profile = analyzer.analyze(tree, buildFiles);

        assertEquals("Sin build.gradle debe devolver framework NONE",
            Framework.NONE, profile.framework());
    }

    // ═══════════════════════════════════════════
    // Tests BuildFileAnalyzer — patrón Strategy
    // ═══════════════════════════════════════════

    @Test
    public void testMavenAnalyzerSupportsOnlyMaven() {
        MavenAnalyzer analyzer = new MavenAnalyzer();
        assertTrue(analyzer.supports(BuildTool.MAVEN));
        assertFalse(analyzer.supports(BuildTool.GRADLE));
    }

    @Test
    public void testGradleAnalyzerSupportsOnlyGradle() {
        GradleAnalyzer analyzer = new GradleAnalyzer();
        assertTrue(analyzer.supports(BuildTool.GRADLE));
        assertFalse(analyzer.supports(BuildTool.MAVEN));
    }
}