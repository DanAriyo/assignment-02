package it.unibo.part02;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import it.unibo.FSReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.BaseStream;
import java.util.concurrent.CountDownLatch;

public class FSStatLibRx {

    private static int getBandIndex(long fileSize, long maxFS, int nb) {
        if (fileSize > maxFS) return nb;
        double bandWidth = (double) maxFS / nb;
        int bandIndex = (int) (fileSize / bandWidth);
        if (bandIndex >= nb) return nb - 1;
        return bandIndex;
    }

    // Il metodo ora è void e non restituisce nulla
    public static void getFSReport(String rootPath, long maxFS, int nb) {
        if (nb <= 0 || maxFS <= 0) {
            System.err.println("Errore: NB e MaxFS devono essere maggiori di zero.");
            return;
        }

        // Questo ci serve SOLO per il test da terminale: impedisce al main di morire prima della stampa
        CountDownLatch latch = new CountDownLatch(1);

        Observable<Path> fileObservable = Observable.using(
                () -> Files.walk(Path.of(rootPath)),
                Observable::fromStream,
                BaseStream::close
        );

        FSReport reportIniziale = new FSReport(0, new long[nb + 1]);

        fileObservable
                .subscribeOn(Schedulers.io())
                .filter(Files::isRegularFile)
                .reduce(reportIniziale, (reportAccumulato, path) -> {
                    try {
                        long size = Files.size(path);
                        int bandaAppartenenza = getBandIndex(size, maxFS, nb);

                        long nuovoTotale = reportAccumulato.getTotalFiles() + 1;
                        long[] nuoveBande = reportAccumulato.getSizeBands().clone();
                        nuoveBande[bandaAppartenenza]++;

                        return new FSReport(nuovoTotale, nuoveBande);
                    } catch (IOException e) {
                        throw new RuntimeException("Impossibile leggere: " + path, e);
                    }
                })
                .subscribe(
                        reportFinale -> {
                            // --- LA TUA STAMPA INTEGRATA ---
                            System.out.println("==================================================");
                            System.out.println("                 REPORT FINALE                    ");
                            System.out.println("==================================================");
                            System.out.printf(">> TOTALE FILE ANALIZZATI: %d%n%n", reportFinale.getTotalFiles());
                            System.out.println("DISTRIBUZIONE DELLE GRANDEZZE:");
                            System.out.println("--------------------------------------------------");

                            long[] bands = reportFinale.getSizeBands();
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

                            latch.countDown(); // Sblocca il main: abbiamo stampato tutto!
                        },
                        errore -> {
                            System.err.println("\n[ERRORE CRITICO] Impossibile completare l'analisi.");
                            System.err.println("Dettaglio: " + errore.getMessage());
                            System.out.println("==================================================");

                            latch.countDown(); // Sblocca il main anche in caso di errore
                        }
                );

        // Il thread principale si ferma qui ad aspettare che il subscribe chiami latch.countDown()
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}