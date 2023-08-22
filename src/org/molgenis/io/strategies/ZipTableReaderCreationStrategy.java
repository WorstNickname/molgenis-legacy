package org.molgenis.io.strategies;

import org.molgenis.io.TableReader;
import org.molgenis.io.ZipTableReader;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public class ZipTableReaderCreationStrategy implements TableReaderCreationStrategy {

    @Override
    public TableReader createTableReader(File file, String fileName) throws IOException {
        return new ZipTableReader(new ZipFile(file));
    }
}
