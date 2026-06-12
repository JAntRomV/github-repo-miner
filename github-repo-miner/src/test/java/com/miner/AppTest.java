package com.miner;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class AppTest {

    // Verifica que los lenguajes objetivo son exactamente los requeridos
    @Test
    public void testLanguagesContainRequiredTargets() {
        SearchFilters filters = new SearchFilters();
        List<String> langs = filters.getLanguages();
        assertTrue("Debe contener Java", langs.contains("Java"));
        assertTrue("Debe contener Python", langs.contains("Python"));
        assertTrue("Debe contener JavaScript", langs.contains("JavaScript"));
        assertEquals("Solo deben existir 3 lenguajes", 3, langs.size());
    }

    // Verifica que el mínimo de commits es un valor positivo y válido
    @Test
    public void testMinCommitsIsPositive() {
        SearchFilters filters = new SearchFilters();
        assertTrue("minCommits debe ser mayor a 0", filters.getMinCommits() > 0);
    }

    // Verifica que los frameworks requeridos están configurados
    @Test
    public void testTechnicalFrameworksArePresent() {
       SearchFilters filters = new SearchFilters();
       List<String> fw = filters.getTechnicalFrameworks();
        assertFalse("La lista de frameworks no debe estar vacía", fw.isEmpty());
        assertTrue("Debe incluir spring-boot", fw.contains("spring-boot"));
        assertTrue("Debe incluir micronaut", fw.contains("micronaut"));
}
}