package it.unibo.part02;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import it.unibo.FSReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.BaseStream;

public class FSStatLibRx {

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

        Observable<Path> fileObservable = Observable.using(
                () -> Files.walk(Path.of(rootPath)),
                Observable::fromStream,
                BaseStream::close
        );

        FSReport reportIniziale = new FSReport(0, new long[nb + 1]);

        return fileObservable
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
                .blockingGet();
    }
}