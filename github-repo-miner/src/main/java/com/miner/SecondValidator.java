package com.miner;

public class SecondValidator {

    private int totalProcesados       = 0;
    private int descartadosPorCommits = 0;
    private int descartadosPorLicencia = 0;
    private int totalAprobados        = 0;

    public boolean validate(EnrichedRepositoryData repo, SearchFilters filters) {
        totalProcesados++;

        // Filtro 1: Verificar mínimo de commits
        if (repo.getCommitCount() < filters.getMinCommits()) {
            descartadosPorCommits++;
            System.out.println("  [Filtro 1 - Commits insuficientes] ❌ " + repo.getFullName()
                + " (" + repo.getCommitCount() + " commits, mínimo requerido: "
                + filters.getMinCommits() + ")");
            return false;
        }

        // Filtro 2: Debe tener licencia open source reconocida
        if (repo.getLicense() == null
                || repo.getLicense().isEmpty()
                || repo.getLicense().equals("No license")) {
            descartadosPorLicencia++;
            System.out.println("  [Filtro 2 - Sin licencia] ❌ " + repo.getFullName());
            return false;
        }

        totalAprobados++;
        System.out.println("  [Validación 2 OK] ✅ " + repo.getFullName()
            + " | commits=" + repo.getCommitCount()
            + " | licencia=" + repo.getLicense()
            + " | watchers=" + repo.getWatchersCount());
        return true;
    }

    public void printReport() {
        System.out.println("\n  === REPORTE DE SEGUNDA VALIDACIÓN ===");
        System.out.println("  Total procesados:                          " + totalProcesados);
        System.out.println("  Descartados Filtro 1 (commits < mínimo):   " + descartadosPorCommits);
        System.out.println("  Descartados Filtro 2 (sin licencia):        " + descartadosPorLicencia);
        System.out.println("  Total aprobados:                            " + totalAprobados);
    }
}