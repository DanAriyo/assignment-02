package it.unibo.part01;

import java.util.Arrays;

public class FSReport {
    private final long totalFiles;
    private final long[] sizeBands; // NB + 1 elementi

    public FSReport(long totalFiles, long[] sizeBands) {
        this.totalFiles = totalFiles;
        this.sizeBands = sizeBands;
    }

    public long getTotalFiles() { return totalFiles; }
    public long[] getSizeBands() { return sizeBands; }

    @Override
    public String toString() {
        return "FSReport{" +
                "Total Files=" + totalFiles +
                ", Bands Distribution=" + Arrays.toString(sizeBands) +
                '}';
    }
}
