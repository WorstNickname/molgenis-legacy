package org.molgenis.io.strategies;

import org.molgenis.io.TableReader;
import org.molgenis.io.excel.ExcelReader;

import java.io.File;
import java.io.IOException;

public class XlsTableReaderCreationStrategy implements TableReaderCreationStrategy {

    @Override
    public TableReader createTableReader(File file, String fileName) throws IOException {
        return new ExcelReader(file);
    }
}
