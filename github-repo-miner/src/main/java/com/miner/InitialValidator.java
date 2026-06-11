package com.miner;

import org.kohsuke.github.GHRepository;

public class InitialValidator {

    public boolean validate(GHRepository repo, SearchFilters filters) {
        try {
            // Validación 1: Descartar repositorios archivados 
            if (repo.isArchived()) {
                System.out.println("Repositorio descartado (está archivado): " + repo.getFullName());
                return false;
            }

            // Validación 2: Descartar repositorios vacíos o con tamaño 0
            if (repo.getSize() == 0) {
                System.out.println("Repositorio descartado (está vacío): " + repo.getFullName());
                return false;
            }

            // Si pasa los primeros filtros del contenido
            System.out.println("Repositorio aprobado en validación inicial: " + repo.getFullName());
            return true;

        } catch (Exception e) {
            System.err.println("Error al validar repositorio: " + e.getMessage());
            return false;
        }
    }
}