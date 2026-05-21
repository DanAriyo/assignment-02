package it.unibo.part01;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import java.util.ArrayList;
import java.util.List;

public class FSStatLibVertx {

    public static Future<FSReport> getFSReport(Vertx vertx, String rootPath, long maxFS, int nb) {
        FileSystem fs = vertx.fileSystem();

        return fs.readDir(rootPath).compose(files -> {
            // Lista di Future che conterranno i singoli report parziali dei file/sottocartelle
            List<Future<FSReport>> futures = new ArrayList<>();

            for (String path : files) {
                futures.add(fs.props(path).compose(props -> {
                    if (props.isDirectory()) {
                        // RICORSIONE: Passiamo i parametri maxFS e nb anche alle sottocartelle
                        return getFSReport(vertx, path, maxFS, nb);
                    } else {
                        // FILE REGOLARE: Calcoliamo in quale banda si colloca questo singolo file
                        long fileSize = props.size();
                        long[] singleFileBands = new long[nb + 1];

                        // Calcoliamo la larghezza di una singola banda (usando i double per evitare troncamenti)
                        double bandWidth = (double) maxFS / nb;

                        if (fileSize > maxFS) {
                            // Finisce nell'ultima fascia extra (indice NB)
                            singleFileBands[nb] = 1;
                        } else {
                            // Calcoliamo l'indice della banda di appartenenza
                            int bandIndex = (int) (fileSize / bandWidth);
                            // Gestiamo il caso limite in cui fileSize sia esattamente uguale a maxFS
                            if (bandIndex >= nb) {
                                bandIndex = nb - 1;
                            }
                            singleFileBands[bandIndex] = 1;
                        }

                        // Restituiamo un report parziale contenente 1 file e la sua banda marcata
                        return Future.succeededFuture(new FSReport(1, singleFileBands));
                    }
                }));
            }

            // Quando tutte le sotto-analisi asincrone hanno risposto, aggreghiamo i dati
            return Future.all(futures).map(compositeFuture -> {
                long totalFilesAccumulator = 0;
                long[] aggregatedBands = new long[nb + 1];

                List<FSReport> subReports = compositeFuture.list();
                for (FSReport report : subReports) {
                    totalFilesAccumulator += report.getTotalFiles();

                    // Sommiamo i contatori delle bande di tutti i sotto-report
                    for (int i = 0; i < aggregatedBands.length; i++) {
                        aggregatedBands[i] += report.getSizeBands()[i];
                    }
                }

                return new FSReport(totalFilesAccumulator, aggregatedBands);
            });
        });
    }
}