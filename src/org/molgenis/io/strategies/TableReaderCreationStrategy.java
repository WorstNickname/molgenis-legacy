package org.molgenis.io.strategies;

import org.molgenis.io.TableReader;

import java.io.File;
import java.io.IOException;

public interface TableReaderCreationStrategy {

    TableReader createTableReader(File file, String fileName) throws IOException;
}
