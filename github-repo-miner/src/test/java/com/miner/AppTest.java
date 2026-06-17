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
}