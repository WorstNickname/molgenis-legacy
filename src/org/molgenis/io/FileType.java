package org.molgenis.io;

public enum FileType {

    CSV("csv"),
    TXT("txt"),
    TSV("tsv"),
    XLS("xls"),
    XLSX("xlsx"),
    ZIP("zip");

    private final String extension;

    FileType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
