package org.molgenis.io.strategies;

import org.apache.commons.io.FilenameUtils;
import org.molgenis.io.SingleTableReader;
import org.molgenis.io.TableReader;
import org.molgenis.io.csv.CsvReader;

import java.io.File;
import java.io.FileNotFoundException;

public class TxtTableReaderCreationStrategy implements TableReaderCreationStrategy {

    @Override
    public TableReader createTableReader(File file, String fileName) throws FileNotFoundException {
        String tableName = FilenameUtils.getBaseName(fileName);
        return new SingleTableReader(new CsvReader(file), tableName);
    }
}
