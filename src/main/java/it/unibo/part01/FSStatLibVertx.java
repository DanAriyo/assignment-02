package it.unibo.part01;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import it.unibo.FSReport;

import java.util.ArrayList;
import java.util.List;

public class FSStatLibVertx {

    public static Future<FSReport> getFSReport(Vertx vertx, String rootPath, long maxFS, int nb) {
        FileSystem fs = vertx.fileSystem();

        return fs.readDir(rootPath).compose(files -> {
            List<Future<FSReport>> futures = new ArrayList<>();

            for (String path : files) {
                futures.add(fs.props(path).compose(props -> {
                    if (props.isDirectory()) {
                        return getFSReport(vertx, path, maxFS, nb);
                    } else {
                        long fileSize = props.size();
                        long[] singleFileBands = new long[nb + 1];

                        double bandWidth = (double) maxFS / nb;

                        if (fileSize > maxFS) {
                            singleFileBands[nb] = 1;
                        } else {
                            int bandIndex = (int) (fileSize / bandWidth);
                            if (bandIndex >= nb) {
                                bandIndex = nb - 1;
                            }
                            singleFileBands[bandIndex] = 1;
                        }

                        return Future.succeededFuture(new FSReport(1, singleFileBands));
                    }
                }));
            }

            return Future.all(futures).map(compositeFuture -> {
                long totalFilesAccumulator = 0;
                long[] aggregatedBands = new long[nb + 1];

                List<FSReport> subReports = compositeFuture.list();
                for (FSReport report : subReports) {
                    totalFilesAccumulator += report.getTotalFiles();

                    for (int i = 0; i < aggregatedBands.length; i++) {
                        aggregatedBands[i] += report.getSizeBands()[i];
                    }
                }

                return new FSReport(totalFilesAccumulator, aggregatedBands);
            });
        });
    }
}