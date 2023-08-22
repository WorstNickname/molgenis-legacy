package org.molgenis.io;

import org.molgenis.io.strategies.TableReaderCreationStrategyContext;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TableReaderFactory {

    private final static TableReaderCreationStrategyContext TABLE_READER_CREATION_STRATEGY_CONTEXT = new TableReaderCreationStrategyContext();

    private TableReaderFactory() {
    }

    public static TableReader create(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file is null");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("file is not a file: " + file.getName());
        }
        return createTableReader(file);
    }

    public static TableReader create(List<File> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files is null or empty");
        }

        AggregateTableReader tableReader = new AggregateTableReader();
        for (File file : files) {
            tableReader.addTableReader(createTableReader(file));
        }

        return tableReader;
    }

    private static TableReader createTableReader(File file) throws IOException {
        return TABLE_READER_CREATION_STRATEGY_CONTEXT.proceed(file);
    }
}
