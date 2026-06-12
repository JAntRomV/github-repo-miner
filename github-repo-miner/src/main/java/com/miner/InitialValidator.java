package com.miner;

import org.kohsuke.github.GHRepository;

public class InitialValidator {

    // Contadores por filtro
    private int totalProcesados   = 0;
    private int descartadosArch   = 0;
    private int descartadosVacios = 0;
    private int totalAprobados    = 0;

    public boolean validate(GHRepository repo, SearchFilters filters) {
        try {
            totalProcesados++;

            // Filtro 1: Descartar repositorios archivados
            if (repo.isArchived()) {
                descartadosArch++;
                System.out.println("  [Filtro 1 - Archivado] " + repo.getFullName());
                return false;
            }

            // Filtro 2: Descartar repositorios vacíos
            if (repo.getSize() == 0) {
                descartadosVacios++;
                System.out.println("  [Filtro 2 - Vacío]   " + repo.getFullName());
                return false;
            }

            totalAprobados++;
            System.out.println("  [Validación OK] " + repo.getFullName());
            return true;

        } catch (Exception e) {
            System.err.println("Error al validar repositorio: " + e.getMessage());
            return false;
        }
    }

    // Imprime resumen de cuántos pasaron cada filtro
    public void printReport() {
        System.out.println("\n  === REPORTE DE VALIDACIÓN INICIAL ===");
        System.out.println("  Total procesados:              " + totalProcesados);
        System.out.println("  Descartados por Filtro 1 (archivados): " + descartadosArch);
        System.out.println("  Descartados por Filtro 2 (vacíos):     " + descartadosVacios);
        System.out.println("  Total aprobados:               " + totalAprobados);
    }

    // Resetea contadores entre búsquedas de cada topic
    public void resetCounters() {
        totalProcesados   = 0;
        descartadosArch   = 0;
        descartadosVacios = 0;
        totalAprobados    = 0;
    }
}