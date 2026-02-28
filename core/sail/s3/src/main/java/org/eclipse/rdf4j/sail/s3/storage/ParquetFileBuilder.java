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
import java.util.HashMap;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.conf.ParquetConfiguration;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;

/**
 * Writes quad entries to a Parquet file in memory and returns the serialized bytes.
 *
 * <p>
 * This builder uses Parquet's {@link OutputFile} API to avoid Hadoop filesystem dependencies. Entries should already be
 * sorted by the caller according to the specified {@link ParquetSchemas.SortOrder}.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * List&lt;QuadEntry&gt; entries = ...;
 * byte[] parquetBytes = ParquetFileBuilder.build(entries, SortOrder.SPOC);
 * </pre>
 */
public final class ParquetFileBuilder {

	/** Default row group size: 8 MiB. */
	private static final int DEFAULT_ROW_GROUP_SIZE = 8 * 1024 * 1024;

	/** Default page size: 64 KiB. */
	private static final int DEFAULT_PAGE_SIZE = 64 * 1024;

	private ParquetFileBuilder() {
		// utility class
	}

	/**
	 * Builds a Parquet file from the given entries using default settings.
	 *
	 * <p>
	 * Uses {@link ParquetSchemas#QUAD_SCHEMA}, 8 MiB row group size, and 64 KiB page size.
	 *
	 * @param entries   the quad entries to write (must already be sorted)
	 * @param sortOrder the sort order of the entries
	 * @return the serialized Parquet file as a byte array
	 */
	public static byte[] build(List<QuadEntry> entries, ParquetSchemas.SortOrder sortOrder) {
		return build(entries, ParquetSchemas.QUAD_SCHEMA, sortOrder,
				DEFAULT_ROW_GROUP_SIZE, DEFAULT_PAGE_SIZE);
	}

	/**
	 * Builds a Parquet file from the given entries with full control over parameters.
	 *
	 * @param entries      the quad entries to write (must already be sorted)
	 * @param schema       the Parquet schema to use
	 * @param sortOrder    the sort order of the entries
	 * @param rowGroupSize the row group size in bytes
	 * @param pageSize     the page size in bytes
	 * @return the serialized Parquet file as a byte array
	 */
	public static byte[] build(List<QuadEntry> entries, MessageType schema,
			ParquetSchemas.SortOrder sortOrder,
			int rowGroupSize, int pageSize) {
		try {
			ByteArrayOutputFile outputFile = new ByteArrayOutputFile();

			try (ParquetWriter<QuadEntry> writer = new QuadEntryWriterBuilder(outputFile, schema)
					.withConf(new PlainParquetConfiguration())
					.withCodecFactory(SimpleCodecFactory.INSTANCE)
					.withCompressionCodec(CompressionCodecName.ZSTD)
					.withRowGroupSize(rowGroupSize)
					.withPageSize(pageSize)
					.withDictionaryEncoding(true)
					.build()) {
				for (QuadEntry entry : entries) {
					writer.write(entry);
				}
			}

			return outputFile.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to build Parquet file", e);
		}
	}

	/**
	 * Custom {@link WriteSupport} that writes {@link QuadEntry} records to Parquet.
	 */
	private static class QuadEntryWriteSupport extends WriteSupport<QuadEntry> {

		private final MessageType schema;
		private RecordConsumer recordConsumer;

		QuadEntryWriteSupport(MessageType schema) {
			this.schema = schema;
		}

		@Override
		public WriteContext init(Configuration configuration) {
			return new WriteContext(schema, new HashMap<>());
		}

		@Override
		public WriteContext init(ParquetConfiguration configuration) {
			return new WriteContext(schema, new HashMap<>());
		}

		@Override
		public void prepareForWrite(RecordConsumer recordConsumer) {
			this.recordConsumer = recordConsumer;
		}

		@Override
		public void write(QuadEntry entry) {
			recordConsumer.startMessage();

			int fieldIndex = 0;

			// subject
			recordConsumer.startField(ParquetSchemas.COL_SUBJECT, fieldIndex);
			recordConsumer.addLong(entry.subject);
			recordConsumer.endField(ParquetSchemas.COL_SUBJECT, fieldIndex);
			fieldIndex++;

			// predicate
			recordConsumer.startField(ParquetSchemas.COL_PREDICATE, fieldIndex);
			recordConsumer.addLong(entry.predicate);
			recordConsumer.endField(ParquetSchemas.COL_PREDICATE, fieldIndex);
			fieldIndex++;

			// object
			recordConsumer.startField(ParquetSchemas.COL_OBJECT, fieldIndex);
			recordConsumer.addLong(entry.object);
			recordConsumer.endField(ParquetSchemas.COL_OBJECT, fieldIndex);
			fieldIndex++;

			// context
			recordConsumer.startField(ParquetSchemas.COL_CONTEXT, fieldIndex);
			recordConsumer.addLong(entry.context);
			recordConsumer.endField(ParquetSchemas.COL_CONTEXT, fieldIndex);
			fieldIndex++;

			// flag
			recordConsumer.startField(ParquetSchemas.COL_FLAG, fieldIndex);
			recordConsumer.addInteger(entry.flag);
			recordConsumer.endField(ParquetSchemas.COL_FLAG, fieldIndex);

			recordConsumer.endMessage();
		}
	}

	/**
	 * Builder for creating a {@link ParquetWriter} that writes {@link QuadEntry} records. Uses
	 * {@link PlainParquetConfiguration} to avoid Hadoop runtime dependencies.
	 */
	private static class QuadEntryWriterBuilder
			extends ParquetWriter.Builder<QuadEntry, QuadEntryWriterBuilder> {

		private final MessageType schema;

		QuadEntryWriterBuilder(OutputFile file, MessageType schema) {
			super(file);
			this.schema = schema;
		}

		@Override
		protected QuadEntryWriterBuilder self() {
			return this;
		}

		@Override
		protected WriteSupport<QuadEntry> getWriteSupport(Configuration conf) {
			return new QuadEntryWriteSupport(schema);
		}

		@Override
		protected WriteSupport<QuadEntry> getWriteSupport(ParquetConfiguration conf) {
			return new QuadEntryWriteSupport(schema);
		}
	}
}
