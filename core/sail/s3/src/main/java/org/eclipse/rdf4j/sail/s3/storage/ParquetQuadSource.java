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
import java.util.ArrayList;
import java.util.List;

import org.apache.parquet.ParquetReadOptions;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;

/**
 * A {@link RawEntrySource} that reads entries from an in-memory Parquet file. Entries are sorted according to the
 * file's sort order and emitted as 4-varint-encoded byte[] keys with 1-byte flag values.
 *
 * <p>
 * The key format encodes all four quad components (s, p, o, c) as varints in the order defined by the
 * {@link QuadIndex}.
 * </p>
 */
public class ParquetQuadSource implements RawEntrySource {

	private final List<Entry> entries;
	private int pos;

	/**
	 * Creates a source from Parquet file bytes.
	 *
	 * @param parquetData the complete Parquet file as byte[]
	 * @param quadIndex   the quad index defining the key encoding order
	 */
	public ParquetQuadSource(byte[] parquetData, QuadIndex quadIndex) {
		this(parquetData, quadIndex, -1, -1, -1, -1);
	}

	/**
	 * Creates a source from Parquet file bytes with filtering.
	 *
	 * @param parquetData the complete Parquet file as byte[]
	 * @param quadIndex   the quad index defining the key encoding order
	 * @param subject     subject filter, or -1 for wildcard
	 * @param predicate   predicate filter, or -1 for wildcard
	 * @param object      object filter, or -1 for wildcard
	 * @param context     context filter, or -1 for wildcard
	 */
	public ParquetQuadSource(byte[] parquetData, QuadIndex quadIndex,
			long subject, long predicate, long object, long context) {
		this.entries = readAllEntries(parquetData, quadIndex, subject, predicate, object, context);
		this.pos = 0;
	}

	@Override
	public boolean hasNext() {
		return pos < entries.size();
	}

	@Override
	public byte[] peekKey() {
		return entries.get(pos).key;
	}

	@Override
	public byte peekFlag() {
		return entries.get(pos).flag;
	}

	@Override
	public void advance() {
		pos++;
	}

	private static List<Entry> readAllEntries(byte[] parquetData, QuadIndex quadIndex,
			long filterS, long filterP, long filterO, long filterC) {
		List<Entry> result = new ArrayList<>();
		ByteArrayInputFile inputFile = new ByteArrayInputFile(parquetData);

		try (ParquetFileReader reader = ParquetFileReader.open(inputFile,
				new ParquetReadOptions.Builder(new PlainParquetConfiguration())
						.withCodecFactory(SimpleCodecFactory.INSTANCE)
						.build())) {
			MessageType schema = reader.getFooter().getFileMetaData().getSchema();
			MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);

			PageReadStore pages;
			while ((pages = reader.readNextRowGroup()) != null) {
				long rows = pages.getRowCount();
				RecordReader<Group> recordReader = columnIO.getRecordReader(pages,
						new GroupRecordConverter(schema));

				for (long i = 0; i < rows; i++) {
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

					byte[] key = quadIndex.toKeyBytes(subject, predicate, object, context);
					result.add(new Entry(key, (byte) flag));
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read Parquet file", e);
		}

		return result;
	}

	private static class Entry {
		final byte[] key;
		final byte flag;

		Entry(byte[] key, byte flag) {
			this.key = key;
			this.flag = flag;
		}
	}
}
