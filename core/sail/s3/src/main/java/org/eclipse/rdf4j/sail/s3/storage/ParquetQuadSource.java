/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.parquet.ParquetReadOptions;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.column.statistics.LongStatistics;
import org.apache.parquet.column.statistics.Statistics;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.BlockMetaData;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;

/**
 * A {@link RawEntrySource} that streams entries from an in-memory Parquet file. Entries are read one row group at a
 * time, and rows within each row group are read lazily.
 *
 * <p>
 * The key format encodes all four quad components (s, p, o, c) as varints in the order defined by the
 * {@link QuadIndex}.
 * </p>
 */
public class ParquetQuadSource implements RawEntrySource {

	private final ParquetFileReader reader;
	private final MessageType schema;
	private final MessageColumnIO columnIO;
	private final QuadIndex quadIndex;
	private final long filterS, filterP, filterO, filterC;
	private final List<BlockMetaData> rowGroups;
	private int rowGroupIndex;

	private RecordReader<Group> recordReader;
	private long remainingRows;
	private byte[] nextKey;
	private byte nextFlag;
	private boolean closed;

	/**
	 * Creates a streaming source from Parquet file bytes.
	 */
	public ParquetQuadSource(byte[] parquetData, QuadIndex quadIndex) {
		this(parquetData, quadIndex, -1, -1, -1, -1);
	}

	/**
	 * Creates a streaming source from Parquet file bytes with filtering.
	 */
	public ParquetQuadSource(byte[] parquetData, QuadIndex quadIndex,
			long subject, long predicate, long object, long context) {
		this.quadIndex = quadIndex;
		this.filterS = subject;
		this.filterP = predicate;
		this.filterO = object;
		this.filterC = context;

		try {
			ByteArrayInputFile inputFile = new ByteArrayInputFile(parquetData);
			this.reader = ParquetFileReader.open(inputFile,
					new ParquetReadOptions.Builder(new PlainParquetConfiguration())
							.withCodecFactory(SimpleCodecFactory.INSTANCE)
							.build());
			this.schema = reader.getFooter().getFileMetaData().getSchema();
			this.columnIO = new ColumnIOFactory().getColumnIO(schema);
			this.rowGroups = reader.getRowGroups();
			this.rowGroupIndex = 0;
			this.remainingRows = 0;
			this.recordReader = null;

			// Buffer the first matching entry
			advanceToNext();
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to open Parquet file for streaming", e);
		}
	}

	@Override
	public boolean hasNext() {
		return nextKey != null;
	}

	@Override
	public byte[] peekKey() {
		return nextKey;
	}

	@Override
	public byte peekFlag() {
		return nextFlag;
	}

	@Override
	public void advance() {
		advanceToNext();
	}

	@Override
	public void close() {
		if (!closed) {
			closed = true;
			try {
				reader.close();
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to close Parquet reader", e);
			}
		}
	}

	private void advanceToNext() {
		nextKey = null;
		try {
			while (true) {
				// Read rows from current row group
				while (remainingRows > 0) {
					remainingRows--;
					Group group = recordReader.read();
					long subject = group.getLong(ParquetSchemas.COL_SUBJECT, 0);
					long predicate = group.getLong(ParquetSchemas.COL_PREDICATE, 0);
					long object = group.getLong(ParquetSchemas.COL_OBJECT, 0);
					long context = group.getLong(ParquetSchemas.COL_CONTEXT, 0);
					int flag = group.getInteger(ParquetSchemas.COL_FLAG, 0);

					long[] quad = { subject, predicate, object, context };
					if (!QuadIndex.matches(quad, filterS, filterP, filterO, filterC)) {
						continue;
					}

					nextKey = quadIndex.toKeyBytes(subject, predicate, object, context);
					nextFlag = (byte) flag;
					return;
				}

				// Move to next row group
				if (!loadNextRowGroup()) {
					return;
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read Parquet row", e);
		}
	}

	private boolean loadNextRowGroup() throws IOException {
		while (rowGroupIndex < rowGroups.size()) {
			BlockMetaData block = rowGroups.get(rowGroupIndex);
			rowGroupIndex++;

			// Row group filtering: check column statistics
			if (!rowGroupMayMatch(block)) {
				reader.skipNextRowGroup();
				continue;
			}

			PageReadStore pages = reader.readNextRowGroup();
			if (pages == null) {
				continue;
			}

			remainingRows = pages.getRowCount();
			recordReader = columnIO.getRecordReader(pages, new GroupRecordConverter(schema));
			return true;
		}
		return false;
	}

	/**
	 * Checks whether a row group's column statistics allow a match against the current filter. If a bound filter value
	 * falls outside a column's [min, max] range, the entire row group can be skipped.
	 */
	private boolean rowGroupMayMatch(BlockMetaData block) {
		for (ColumnChunkMetaData col : block.getColumns()) {
			Statistics<?> stats = col.getStatistics();
			if (stats == null || stats.isEmpty() || !stats.hasNonNullValue()) {
				continue;
			}
			if (!(stats instanceof LongStatistics)) {
				continue;
			}
			LongStatistics longStats = (LongStatistics) stats;
			long min = longStats.getMin();
			long max = longStats.getMax();

			String colName = col.getPath().toDotString();
			long filterVal = getFilterForColumn(colName);
			if (filterVal >= 0 && (filterVal < min || filterVal > max)) {
				return false;
			}
		}
		return true;
	}

	private long getFilterForColumn(String colName) {
		switch (colName) {
		case ParquetSchemas.COL_SUBJECT:
			return filterS;
		case ParquetSchemas.COL_PREDICATE:
			return filterP;
		case ParquetSchemas.COL_OBJECT:
			return filterO;
		case ParquetSchemas.COL_CONTEXT:
			return filterC;
		default:
			return -1;
		}
	}
}
