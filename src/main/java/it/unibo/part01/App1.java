package it.unibo.part01;

import io.vertx.core.Vertx;

public class App1 {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        long maxFS = 10 * 1024 * 1024; // 10 Megabyte in bytes
        int nb = 4;                   // Dividiamo in 4 bande (+ 1 per i file più grandi)

        System.out.println("Analisi del File System avviata...");

        FSStatLibVertx.getFSReport(vertx, "src/main/java/it/unibo/lib", maxFS, nb)
                .onSuccess(report -> {
                    System.out.println("\n--- REPORT FINALE ---");
                    System.out.println("Numero totale di file trovati: " + report.getTotalFiles());

                    long[] bands = report.getSizeBands();
                    double bandWidth = (double) maxFS / nb;

                    // Stampiamo la distribuzione in modo leggibile
                    for (int i = 0; i < nb; i++) {
                        long minIdx = (long) (i * bandWidth);
                        long maxIdx = (long) ((i + 1) * bandWidth);
                        System.out.println("Fascia " + i + " [" + minIdx + " - " + maxIdx + " bytes]: " + bands[i] + " file");
                    }
                    System.out.println("Fascia " + nb + " [> " + maxFS + " bytes]: " + bands[nb] + " file");

                    vertx.close();
                })
                .onFailure(err -> {
                    System.err.println("Errore nel calcolo del report: " + err.getMessage());
                    vertx.close();
                });
    }
}