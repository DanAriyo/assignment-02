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

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            try (var pathStream = Files.walk(Path.of(rootPath))) {

                futures = pathStream
                        .filter(Files::isRegularFile)
                        .map(p -> executor.submit(() -> {
                            try {
                                return Files.size(p);
                            } catch (IOException e) {
                                return -1L;
                            }
                        }))
                        .toList();
            }catch (IOException e) {
                throw new RuntimeException("Errore durante l'aggregazione dei risultati dei Virtual Threads", e);
            }

        }

        long totalFilesAccumulator = 0;
        long[] aggregatedBands = new long[nb + 1];

        for (Future<Long> future : futures) {
            try {
                long fileSize = future.get();

                if (fileSize != -1L) {
                    totalFilesAccumulator++;

                    int bandIdx = getBandIndex(fileSize, maxFS, nb);
                    aggregatedBands[bandIdx]++;
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Errore durante l'aggregazione dei risultati dei Virtual Threads", e);
            }
        }

        return new FSReport(totalFilesAccumulator, aggregatedBands);
    }
}