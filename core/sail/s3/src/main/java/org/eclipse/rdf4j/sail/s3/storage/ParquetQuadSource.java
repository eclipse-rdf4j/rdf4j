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
import java.nio.ByteBuffer;
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
 * file's sort order (soc, osc, cso) and emitted as varint-encoded byte[] keys with 1-byte flag values.
 *
 * <p>
 * The key format encodes (value1, value2, value3) as varints in the sort order of the file. For example, an "soc" file
 * produces keys as varint(subject)||varint(object)||varint(context).
 * </p>
 */
public class ParquetQuadSource implements RawEntrySource {

	private final List<Entry> entries;
	private int pos;

	/**
	 * Creates a source from Parquet file bytes.
	 *
	 * @param parquetData the complete Parquet file as byte[]
	 * @param sortOrder   the sort order of the file ("soc", "osc", or "cso")
	 */
	public ParquetQuadSource(byte[] parquetData, String sortOrder) {
		this.entries = readAllEntries(parquetData, sortOrder);
		this.pos = 0;
	}

	/**
	 * Creates a source from Parquet file bytes with filtering.
	 *
	 * @param parquetData the complete Parquet file as byte[]
	 * @param sortOrder   the sort order of the file
	 * @param subject     subject filter, or -1 for wildcard
	 * @param object      object filter, or -1 for wildcard
	 * @param context     context filter, or -1 for wildcard
	 */
	public ParquetQuadSource(byte[] parquetData, String sortOrder, long subject, long object, long context) {
		List<Entry> all = readAllEntries(parquetData, sortOrder);
		if (subject >= 0 || object >= 0 || context >= 0) {
			List<Entry> filtered = new ArrayList<>();
			for (Entry e : all) {
				if ((subject >= 0 && e.subject != subject)
						|| (object >= 0 && e.object != object)
						|| (context >= 0 && e.context != context)) {
					continue;
				}
				filtered.add(e);
			}
			this.entries = filtered;
		} else {
			this.entries = all;
		}
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

	private static List<Entry> readAllEntries(byte[] parquetData, String sortOrder) {
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
					long object = group.getLong(ParquetSchemas.COL_OBJECT, 0);
					long context = group.getLong(ParquetSchemas.COL_CONTEXT, 0);
					int flag = group.getInteger(ParquetSchemas.COL_FLAG, 0);

					byte[] key = encodeKey(sortOrder, subject, object, context);
					result.add(new Entry(key, (byte) flag, subject, object, context));
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read Parquet file", e);
		}

		return result;
	}

	/**
	 * Encodes a key in the given sort order as varints.
	 */
	static byte[] encodeKey(String sortOrder, long subject, long object, long context) {
		long v1, v2, v3;
		switch (sortOrder) {
		case "osc":
			v1 = object;
			v2 = subject;
			v3 = context;
			break;
		case "cso":
			v1 = context;
			v2 = subject;
			v3 = object;
			break;
		case "soc":
		default:
			v1 = subject;
			v2 = object;
			v3 = context;
			break;
		}

		int len = Varint.calcLengthUnsigned(v1) + Varint.calcLengthUnsigned(v2) + Varint.calcLengthUnsigned(v3);
		ByteBuffer bb = ByteBuffer.allocate(len);
		Varint.writeUnsigned(bb, v1);
		Varint.writeUnsigned(bb, v2);
		Varint.writeUnsigned(bb, v3);
		return bb.array();
	}

	private static class Entry {
		final byte[] key;
		final byte flag;
		final long subject;
		final long object;
		final long context;

		Entry(byte[] key, byte flag, long subject, long object, long context) {
			this.key = key;
			this.flag = flag;
			this.subject = subject;
			this.object = object;
			this.context = context;
		}
	}
}
