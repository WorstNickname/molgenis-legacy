package org.molgenis.io;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AggregateTableReader implements TableReader
	{
		private final List<TableReader> tableReaders;
		private final Map<String, TupleReader> tupleReaders;

		public AggregateTableReader()
		{
			tableReaders = new ArrayList<TableReader>();
			tupleReaders = new LinkedHashMap<String, TupleReader>();
		}

		@Override
		public Iterator<TupleReader> iterator()
		{
			return Collections.<TupleReader> unmodifiableCollection(tupleReaders.values()).iterator();
		}

		public void addTableReader(TableReader tableReader) throws IOException
		{
			tableReaders.add(tableReader);
			for (String tableName : tableReader.getTableNames())
				tupleReaders.put(tableName, tableReader.getTupleReader(tableName));
		}

		@Override
		public void close() throws IOException
		{
			for (TableReader tableReader : tableReaders)
				IOUtils.closeQuietly(tableReader);
		}

		@Override
		public TupleReader getTupleReader(String tableName) throws IOException
		{
			return tupleReaders.get(tableName);
		}

		@Override
		public Iterable<String> getTableNames() throws IOException
		{
			return Collections.unmodifiableSet(tupleReaders.keySet());
		}
	}