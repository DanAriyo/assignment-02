package it.unibo;

import io.vertx.core.Vertx;
import it.unibo.part01.FSStatLibVertx;
import it.unibo.part02.FSStatLibRx;
import it.unibo.part03.FSStatLibVT;

public class App {

    private enum EngineMode { VERTX, RX, VT}

    public static void main(String[] args) {
        EngineMode mode = EngineMode.VT;



        long maxFS = 100 * 1024;
        int nb = 4;

        System.out.println("==================================================");
        System.out.println("  AVVIO ANALISI DEL FILE SYSTEM  ");
        System.out.println("==================================================");
        System.out.printf("Modalità attiva: %s%n", mode);
        System.out.printf("Impostazioni: MaxFS = %d KB | Numero Bande (NB) = %d%n", maxFS / 1024, nb);
        System.out.println("Attendere prego...\n");

        String targetPath = "src/main/java/it/unibo/lib";

        switch (mode) {
            case VERTX -> {
                Vertx vertx = Vertx.vertx();
                FSStatLibVertx.getFSReport(vertx, targetPath, maxFS, nb)
                        .onSuccess(report -> {
                            printReport(report, maxFS, nb);
                            vertx.close();
                        })
                        .onFailure(err -> {
                            printError(err);
                            vertx.close();
                        });
            }
            case RX -> {
                try {
                    FSReport report = FSStatLibRx.getFSReport(targetPath, maxFS, nb);
                    printReport(report, maxFS, nb);
                } catch (Exception err) {
                    printError(err);
                }
            }
            case VT -> {
                try {
                    FSReport report = FSStatLibVT.getFSReport(targetPath, maxFS, nb);
                    printReport(report, maxFS, nb);
                } catch (Exception err) {
                    printError(err);
                }
            }
        }
    }

    /**
     * Funzione unica e centralizzata per la stampa formattata del report delle dimensioni.
     */
    private static void printReport(FSReport report, long maxFS, int nb) {
        System.out.println("==================================================");
        System.out.println("                 REPORT FINALE                    ");
        System.out.println("==================================================");
        System.out.printf(">> TOTALE FILE ANALIZZATI: %d%n%n", report.getTotalFiles());
        System.out.println("DISTRIBUZIONE DELLE GRANDEZZE:");
        System.out.println("--------------------------------------------------");

        long[] bands = report.getSizeBands();
        double bandWidth = (double) maxFS / nb;

        for (int i = 0; i < nb; i++) {
            double minKb = (i * bandWidth) / 1024.0;
            double maxKb = ((i + 1) * bandWidth) / 1024.0;

            System.out.printf("Fascia %d [%6.2f KB - %6.2f KB] -> %d file%n",
                    i, minKb, maxKb, bands[i]);
        }

        System.out.printf("Fascia %d [        > %6.2f KB] -> %d file%n",
                nb, maxFS / 1024.0, bands[nb]);
        System.out.println("==================================================");
    }

    /**
     * Sotto-funzione di supporto per uniformare anche la stampa degli errori.
     */
    private static void printError(Throwable err) {
        System.err.println("\n[ERRORE CRITICO] Impossibile completare l'analisi.");
        System.err.println("Dettaglio: " + err.getMessage());
        System.out.println("==================================================");
    }
}