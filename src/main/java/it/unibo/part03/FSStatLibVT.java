package it.unibo.part03;

import it.unibo.FSReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FSStatLibVT {

    // Funzione helper integrata come richiesto
    private static int getBandIndex(long fileSize, long maxFS, int nb) {
        if (fileSize > maxFS) return nb;
        double bandWidth = (double) maxFS / nb;
        int bandIndex = (int) (fileSize / bandWidth);
        if (bandIndex >= nb) return nb - 1;
        return bandIndex;
    }

    public static FSReport getFSReport(String rootPath, long maxFS, int nb) {
        if (nb <= 0 || maxFS <= 0) {
            throw new IllegalArgumentException("NB e MaxFS devono essere maggiori di zero.");
        }

        List<Future<Long>> futures;

        // 1. Configurazione dell'Executor per i Virtual Threads
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // 2. Apertura sicura dello stream di navigazione del File System
            try (var pathStream = Files.walk(Path.of(rootPath))) {

                // Sottomettiamo i task all'executor e forziamo la partenza dello stream con .toList()
                futures = pathStream
                        .filter(Files::isRegularFile)
                        .map(p -> executor.submit(() -> {
                            // Ciascun file viene elaborato in parallelo in un Virtual Thread dedicato
                            try {
                                return Files.size(p);
                            } catch (IOException e) {
                                // Valore sentinella in caso di file non accessibile o rimosso
                                return -1L;
                            }
                        }))
                        .toList();
            }catch (IOException e) {
                throw new RuntimeException("Errore durante l'aggregazione dei risultati dei Virtual Threads", e);
            }

        } // <--- BARRIERA DI SINCRONIZZAZIONE: Il thread principale attende il completamento di tutti i Virtual Threads.

        // 3. Fase di Aggregazione (Eseguita in sicurezza dal thread principale)
        long totalFilesAccumulator = 0;
        long[] aggregatedBands = new long[nb + 1];

        for (Future<Long> future : futures) {
            try {
                // Il get() è immediato e non bloccante perché la barriera è già stata superata
                long fileSize = future.get();

                // Elaboriamo le statistiche solo per i file letti con successo
                if (fileSize != -1L) {
                    totalFilesAccumulator++;

                    // Utilizzo della funzione helper per trovare il secchiello corretto
                    int bandIdx = getBandIndex(fileSize, maxFS, nb);
                    aggregatedBands[bandIdx]++;
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Errore durante l'aggregazione dei risultati dei Virtual Threads", e);
            }
        }

        // 4. Restituzione del Report finale strutturato
        return new FSReport(totalFilesAccumulator, aggregatedBands);
    }
}