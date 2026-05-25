package it.unibo.part02;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.Future;
import it.unibo.FSReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.BaseStream;
import java.util.stream.Stream;

public class FSStatLibRx {

    public static Future<FSReport> getFSReport(){
        Path resourcePath = Paths.get("src/main/java/it/unibo/lib");

        // Usiamo Observable.using per gestire lo stream in modo reattivo e sicuro
        Observable<Path> fileObservable = Observable.using(
                // 1. Risorsa da aprire (il nostro stream di Java NIO)
                () -> Files.walk(resourcePath),

                // 2. Funzione che trasforma la risorsa in un Observable
                Observable::fromStream,

                // 3. Funzione di pulizia: garantisce la chiusura dello stream alla fine
                BaseStream::close
        );

        // Ora puoi concatenare le tue operazioni sul flusso in tutta sicurezza
        fileObservable
                .subscribeOn(Schedulers.io()) // Sposta le letture bloccanti del disco sul thread di I/O
                .filter(Files::isRegularFile) // Tiene solo i file
                .map(path -> {
                    try {
                        return Files.size(path); // Estrae la grandezza
                    } catch (IOException e) {
                        throw new RuntimeException("Errore lettura dimensione", e);
                    }
                })
                // Ci iscriviamo per vedere i risultati
                .subscribe(
                        size -> System.out.println("Size: " + size / 1024 + " KB"),
                        err -> System.err.println("Errore nel flusso: " + err.getMessage()),
                        () -> System.out.println("Analisi completata con successo!")
                );

        // Evita che il main si chiuda subito durante il test asincrono
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        return null;
    }
}
