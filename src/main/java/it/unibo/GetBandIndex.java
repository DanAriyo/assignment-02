package it.unibo;

public class GetBandIndex {

    public static int getBandIndex(long fileSize, long maxFS, int nb) {
        // Se il file supera il limite massimo, va nell'ultimo secchiello extra (indice nb)
        if (fileSize > maxFS) {
            return nb;
        }

        // Calcoliamo la larghezza di una singola banda usando i double per non perdere i decimali
        double bandWidth = (double) maxFS / nb;

        // Dividiamo il peso del file per la larghezza della banda
        int bandIndex = (int) (fileSize / bandWidth);

        // Gestiamo il caso limite: se il file è ESATTAMENTE uguale a maxFS (es. 100),
        // la divisione darebbe 'nb' (4), ma deve stare nell'ultima banda regolare, cioè 'nb - 1' (3).
        if (bandIndex >= nb) {
            return nb - 1;
        }

        return bandIndex;
    }
}
