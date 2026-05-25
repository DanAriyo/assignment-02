package it.unibo;

import io.vertx.core.Vertx;
import it.unibo.part01.FSStatLibVertx;

public class App {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // Configurazione dei parametri del report
        long maxFS = 100 * 1024; // 100 KiloByte
        int nb = 4;              // Dividiamo in 4 bande (+ 1 per i file eccedenti)

        System.out.println("==================================================");
        System.out.println("  AVVIO ANALISI ASINCRONA DEL FILE SYSTEM  ");
        System.out.println("==================================================");
        System.out.printf("Impostazioni: MaxFS = %d KB | Numero Bande (NB) = %d%n", maxFS / 1024, nb);

        FSStatLibVertx.getFSReport(vertx, "src/main/java/it/unibo/lib", maxFS, nb)
                .onSuccess(report -> {
                    System.out.println("==================================================");
                    System.out.println("                 REPORT FINALE                    ");
                    System.out.println("==================================================");
                    System.out.printf(">> TOTALE FILE ANALIZZATI: %d%n%n", report.getTotalFiles());
                    System.out.println("DISTRIBUZIONE DELLE GRANDEZZE:");
                    System.out.println("--------------------------------------------------");

                    long[] bands = report.getSizeBands();
                    double bandWidth = (double) maxFS / nb;

                    // Stampiamo la distribuzione in KB per una migliore leggibilità
                    for (int i = 0; i < nb; i++) {
                        double minKb = (i * bandWidth) / 1024.0;
                        double maxKb = ((i + 1) * bandWidth) / 1024.0;

                        System.out.printf("Fascia %d [%6.2f KB - %6.2f KB] -> %d file%n",
                                i, minKb, maxKb, bands[i]);
                    }

                    // Ultima fascia per i file che superano il MaxFS
                    System.out.printf("Fascia %d [        > %6.2f KB] -> %d file%n",
                            nb, maxFS / 1024.0, bands[nb]);
                    System.out.println("==================================================");

                    vertx.close();
                })
                .onFailure(err -> {
                    System.err.println("\n[ERRORE CRITICO] Impossibile completare l'analisi.");
                    System.err.println("Dettaglio: " + err.getMessage());
                    System.out.println("==================================================");
                    vertx.close();
                });
    }
}