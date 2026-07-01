package com.miner;

import org.kohsuke.github.GHRepository;

public class RepositoryValidator {

    // Contadores Fase 1
    private int p1Procesados = 0;
    private int p1Archivados = 0;
    private int p1Vacios     = 0;
    private int p1Aprobados  = 0;

    // Contadores Fase 2
    private int p2Procesados  = 0;
    private int p2PorCommits  = 0;
    private int p2PorLicencia = 0;
    private int p2Aprobados   = 0;

    // --- VALIDACIÓN FASE 1: archivado y vacío ---
    public boolean validatePhase1(GHRepository repo, SearchFilters filters) {
        try {
            p1Procesados++;

            if (repo.isArchived()) {
                p1Archivados++;
                System.out.println("  [Filtro 1 - Archivado] " + repo.getFullName());
                return false;
            }

            if (repo.getSize() == 0) {
                p1Vacios++;
                System.out.println("  [Filtro 2 - Vacío] " + repo.getFullName());
                return false;
            }

            p1Aprobados++;
            System.out.println("  [Fase 1 OK] " + repo.getFullName());
            return true;

        } catch (Exception e) {
            System.err.println("Error al validar: " + e.getMessage());
            return false;
        }
    }

    // --- VALIDACIÓN FASE 2: commits y licencia ---
    public boolean validatePhase2(RepositoryData repo, SearchFilters filters) {
        p2Procesados++;

        if (repo.getCommitCount() < filters.getMinCommits()) {
            p2PorCommits++;
            System.out.println("  [Filtro commits] ❌ " + repo.getFullName()
                + " (" + repo.getCommitCount() + " commits, mínimo: "
                + filters.getMinCommits() + ")");
            return false;
        }

        if (repo.getLicense() == null
                || repo.getLicense().isEmpty()
                || repo.getLicense().equals("No license")) {
            p2PorLicencia++;
            System.out.println("  [Filtro licencia] ❌ " + repo.getFullName());
            return false;
        }

        p2Aprobados++;
        System.out.println("  [Fase 2 OK] ✅ " + repo.getFullName()
            + " | commits=" + repo.getCommitCount()
            + " | licencia=" + repo.getLicense()
            + " | watchers=" + repo.getWatchersCount());
        return true;
    }

    public void printPhase1Report() {
        System.out.println("\n  === REPORTE FASE 1 ===");
        System.out.println("  Procesados:  " + p1Procesados);
        System.out.println("  Archivados:  " + p1Archivados);
        System.out.println("  Vacíos:      " + p1Vacios);
        System.out.println("  Aprobados:   " + p1Aprobados);
    }

    public void printPhase2Report() {
        System.out.println("\n  === REPORTE FASE 2 ===");
        System.out.println("  Procesados:             " + p2Procesados);
        System.out.println("  Descartados (commits):  " + p2PorCommits);
        System.out.println("  Descartados (licencia): " + p2PorLicencia);
        System.out.println("  Aprobados:              " + p2Aprobados);
    }

    // Resetea solo los contadores de Fase 1 entre búsquedas por topic
    public void resetPhase1Counters() {
        p1Procesados = 0;
        p1Archivados = 0;
        p1Vacios     = 0;
        p1Aprobados  = 0;
    }

    // Contadores Fase 3
private int p3Procesados    = 0;
private int p3SinBuildTool  = 0; // no tienen pom.xml ni build.gradle
private int p3SinFramework  = 0; // no tienen la dependencia del framework
private int p3SinEstructura = 0; // no tienen src/main/java
private int p3Aprobados     = 0;

public boolean validatePhase3(RepositoryData repo) {
    p3Procesados++;

    // FILTRO 1: ¿Tiene un archivo de build reconocido?
    // Si no hay pom.xml ni build.gradle, el repo no es un proyecto Java estándar
    if (repo.getBuildTool() == null || repo.getBuildTool().equals("None")) {
        p3SinBuildTool++;
        System.out.println("  [Filtro 1 - Sin build tool] ❌ " + repo.getFullName());
        return false;
    }

    // FILTRO 2: ¿El build file tiene la dependencia del framework?
    // Verifica que realmente use Spring Boot o Micronaut, no solo que tenga el topic
    if (!repo.isHasFrameworkDependency()) {
        p3SinFramework++;
        System.out.println("  [Filtro 2 - Sin framework dep] ❌ " + repo.getFullName()
            + " (buildTool=" + repo.getBuildTool() + ")");
        return false;
    }

    // FILTRO 3: ¿Tiene la estructura estándar de un proyecto Java?
    // src/main/java es obligatorio en proyectos Maven y Gradle estándar
    if (!repo.isHasSrcMainJava()) {
        p3SinEstructura++;
        System.out.println("  [Filtro 3 - Sin src/main/java] ❌ " + repo.getFullName());
        return false;
    }

    p3Aprobados++;
    System.out.println("  [Fase 3 OK] ✅ " + repo.getFullName()
        + " | " + repo.getBuildTool()
        + " | " + repo.getDetectedFramework());
    return true;
}

public void printPhase3Report() {
    System.out.println("\n  === REPORTE FASE 3 - FILTRO TECNICO ===");
    System.out.println("  Total procesados:                     " + p3Procesados);
    System.out.println("  Descartados Filtro 1 (sin build tool): " + p3SinBuildTool);
    System.out.println("  Descartados Filtro 2 (sin framework):  " + p3SinFramework);
    System.out.println("  Descartados Filtro 3 (sin estructura): " + p3SinEstructura);
    System.out.println("  Total aprobados:                       " + p3Aprobados);
}
}