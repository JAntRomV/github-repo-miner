package com.miner;

/**
 * Perfil técnico consolidado de un repositorio.
 * Es el producto de la Fase 3 — junto con RepoMetadata (Fase 2),
 * es el insumo directo del ScoringEngine (Fase 4).
 */
public record TechProfile(
    BuildTool buildTool,
    Framework framework,
    int javaVersion,
    boolean java21,
    boolean graalvmReady,
    boolean hasTestSuite,
    TestFramework testFramework,
    int testFileCount,
    boolean jmhPresent,
    boolean jmhCandidate,
    boolean profilingCandidate,
    Sector sector,
    boolean travisCi,
    boolean passesHardFilters
) {
    // Perfil vacío para repos donde no se pudo determinar nada (sin build file, error de red, etc.)
    public static TechProfile empty(BuildTool tool) {
        return new TechProfile(tool, Framework.NONE, 0, false, false,
            false, TestFramework.NONE, 0, false, false, false,
            Sector.UNKNOWN, false, false);
    }
}