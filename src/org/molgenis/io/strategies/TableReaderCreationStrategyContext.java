package org.molgenis.io.strategies;

import org.molgenis.io.FileType;
import org.molgenis.io.TableReader;
import org.molgenis.io.strategies.TableReaderCreationStrategy;
import org.molgenis.io.strategies.CsvTableReaderCreationStrategy;
import org.molgenis.io.strategies.TsvTableReaderCreationStrategy;
import org.molgenis.io.strategies.TxtTableReaderCreationStrategy;
import org.molgenis.io.strategies.XlsTableReaderCreationStrategy;
import org.molgenis.io.strategies.ZipTableReaderCreationStrategy;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.io.Files.getFileExtension;
import static org.molgenis.io.FileType.CSV;
import static org.molgenis.io.FileType.TSV;
import static org.molgenis.io.FileType.TXT;
import static org.molgenis.io.FileType.XLS;
import static org.molgenis.io.FileType.XLSX;
import static org.molgenis.io.FileType.ZIP;

public class TableReaderCreationStrategyContext {

    private final Map<FileType, TableReaderCreationStrategy> tableReaderCreationStrategies;

    public TableReaderCreationStrategyContext() {
        this.tableReaderCreationStrategies = init();
    }

    public TableReader proceed(File file) throws IOException {
        String name = file.getName();
        FileType fileType = checkFileExtension(name);
        TableReaderCreationStrategy strategy = getCreationStrategy(fileType);
        return strategy.createTableReader(file, name);
    }

    private static FileType checkFileExtension(String name) throws IOException {
        String inputFileExtension = getFileExtension(name);
        for (FileType fileType : FileType.values()) {
            if (fileType.getExtension().equals(inputFileExtension)) {
                return fileType;
            }
        }
        throw new IOException("unknown file type: " + inputFileExtension);
    }

    private Map<FileType, TableReaderCreationStrategy> init() {
        Map<FileType, TableReaderCreationStrategy> tableReaders = new HashMap<FileType, TableReaderCreationStrategy>();
        tableReaders.put(CSV, new CsvTableReaderCreationStrategy());
        tableReaders.put(TXT, new TxtTableReaderCreationStrategy());
        tableReaders.put(TSV, new TsvTableReaderCreationStrategy());
        tableReaders.put(XLS, new XlsTableReaderCreationStrategy());
        tableReaders.put(XLSX, new XlsTableReaderCreationStrategy());
        tableReaders.put(ZIP, new ZipTableReaderCreationStrategy());
        return tableReaders;
    }

    private TableReaderCreationStrategy getCreationStrategy(FileType fileType) throws IOException {
        TableReaderCreationStrategy strategy = tableReaderCreationStrategies.get(fileType);
        if (strategy == null) {
            throw new IOException("unknown file type: " + fileType.getExtension());
        }
        return strategy;
    }
}