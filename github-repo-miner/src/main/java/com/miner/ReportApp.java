package com.miner;

import java.io.FileWriter;
import java.io.IOException;

public class ReportApp {
    public static void main(String[] args) throws Exception {
        System.out.println("=== ISSUE #4 PUNTO 3: Generación de Reporte ===\n");

        ReportGenerator generator = new ReportGenerator();
        String report = generator.generateMarkdownReport();

        try (FileWriter writer = new FileWriter("reporte_scoring.md")) {
            writer.write(report);
        }

        System.out.println(report);
        System.out.println("\n✅ Reporte guardado en: reporte_scoring.md");
    }
}