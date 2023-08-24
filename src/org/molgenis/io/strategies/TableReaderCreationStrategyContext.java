package org.molgenis.io.strategies;

import org.molgenis.io.FileType;
import org.molgenis.io.TableReader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.io.Files.getFileExtension;

public class TableReaderCreationStrategyContext {

    private final Map<FileType, TableReaderCreationStrategy> tableReaderCreationStrategies;

    public TableReaderCreationStrategyContext() {
        this.tableReaderCreationStrategies = initStrategies();
    }

    public TableReader executeCreationStrategy(File file) throws IOException {
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

    private Map<FileType, TableReaderCreationStrategy> initStrategies() {
        Map<FileType, TableReaderCreationStrategy> tableReaders = new HashMap<FileType, TableReaderCreationStrategy>();
        for (FileType fileType : FileType.values()) {
            TableReaderCreationStrategy strategy = fileType.getStrategy();
            tableReaders.put(fileType, strategy);
        }
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