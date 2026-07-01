package com.miner;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
import java.util.ArrayList;

public class AppTest {

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
    // fase 3 test

    // Tests nuevos de Fase 3
@Test
public void testPhase3ApprovesValidRepo() {
    RepositoryValidator validator = new RepositoryValidator();

    RepositoryData repo = new RepositoryData();
    repo.setBuildTool("Maven");
    repo.setHasFrameworkDependency(true);
    repo.setDetectedFramework("Spring Boot");
    repo.setHasSrcMainJava(true);

    assertTrue("Debe aprobar repo con Maven, framework y estructura",
        validator.validatePhase3(repo));
}

@Test
public void testPhase3RejectsNoBuildTool() {
    RepositoryValidator validator = new RepositoryValidator();

    RepositoryData repo = new RepositoryData();
    repo.setBuildTool("None");
    repo.setHasFrameworkDependency(false);
    repo.setHasSrcMainJava(true);

    assertFalse("Debe rechazar repo sin build tool",
        validator.validatePhase3(repo));
}

@Test
public void testPhase3RejectsNoFrameworkDep() {
    RepositoryValidator validator = new RepositoryValidator();

    RepositoryData repo = new RepositoryData();
    repo.setBuildTool("Gradle");
    repo.setHasFrameworkDependency(false); // tiene Gradle pero no la dep
    repo.setHasSrcMainJava(true);

    assertFalse("Debe rechazar repo sin dependencia del framework",
        validator.validatePhase3(repo));
}

@Test
public void testPhase3RejectsNoSrcMainJava() {
    RepositoryValidator validator = new RepositoryValidator();

    RepositoryData repo = new RepositoryData();
    repo.setBuildTool("Maven");
    repo.setHasFrameworkDependency(true);
    repo.setHasSrcMainJava(false); // no tiene la estructura

    assertFalse("Debe rechazar repo sin src/main/java",
        validator.validatePhase3(repo));
}

@Test
public void testParserDetectsSpringBootInPom() {
    RepositoryParser parser = new RepositoryParser();
    String fakePom = "<dependency><groupId>org.springframework.boot</groupId></dependency>";

    ParseResult result = parser.parsePomXml(fakePom);

    assertTrue("Debe detectar Spring Boot", result.isHasSpringBoot());
    assertEquals("Debe decir Maven", "Maven", result.getBuildTool());
}

@Test
public void testParserDetectsMicronautInGradle() {
    RepositoryParser parser = new RepositoryParser();
    String fakeGradle = "implementation(\"io.micronaut:micronaut-http-server-netty\")";

    ParseResult result = parser.parseBuildGradle(fakeGradle);

    assertTrue("Debe detectar Micronaut", result.isHasMicronaut());
    assertEquals("Debe decir Gradle", "Gradle", result.getBuildTool());
}
}