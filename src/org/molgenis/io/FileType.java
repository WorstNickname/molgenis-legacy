package org.molgenis.io;

import org.molgenis.io.strategies.CsvTableReaderCreationStrategy;
import org.molgenis.io.strategies.TableReaderCreationStrategy;
import org.molgenis.io.strategies.TsvTableReaderCreationStrategy;
import org.molgenis.io.strategies.TxtTableReaderCreationStrategy;
import org.molgenis.io.strategies.XlsTableReaderCreationStrategy;
import org.molgenis.io.strategies.ZipTableReaderCreationStrategy;

public enum FileType {

    CSV("csv", new CsvTableReaderCreationStrategy()),
    TXT("txt", new TxtTableReaderCreationStrategy()),
    TSV("tsv", new TsvTableReaderCreationStrategy()),
    XLS("xls", new XlsTableReaderCreationStrategy()),
    XLSX("xlsx", new XlsTableReaderCreationStrategy()),
    ZIP("zip", new ZipTableReaderCreationStrategy());

    private final String extension;
    private final TableReaderCreationStrategy strategy;

    FileType(String extension, TableReaderCreationStrategy strategy) {
        this.extension = extension;
        this.strategy = strategy;
    }

    public String getExtension() {
        return extension;
    }

    public TableReaderCreationStrategy getStrategy() {
        return strategy;
    }
}
