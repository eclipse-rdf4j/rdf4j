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
 * byte[] parquetBytes = ParquetFileBuilder.build(entries, SortOrder.SOC);
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
	 * A quad entry to be written to a Parquet file.
	 *
	 * <p>
	 * For partitioned schemas the predicate is implicit in the partition path, so only subject, object, context, and
	 * flag are stored. For unpartitioned schemas, the predicate field is also written.
	 */
	public static class QuadEntry {
		public final long subject;
		public final long predicate;
		public final long object;
		public final long context;
		public final byte flag;

		/**
		 * Creates a quad entry for partitioned files (predicate implicit in path).
		 *
		 * @param subject the subject value ID
		 * @param object  the object value ID
		 * @param context the context value ID
		 * @param flag    the entry flag (e.g. insert vs tombstone)
		 */
		public QuadEntry(long subject, long object, long context, byte flag) {
			this(subject, -1, object, context, flag);
		}

		/**
		 * Creates a quad entry for unpartitioned files (predicate stored explicitly).
		 *
		 * @param subject   the subject value ID
		 * @param predicate the predicate value ID
		 * @param object    the object value ID
		 * @param context   the context value ID
		 * @param flag      the entry flag (e.g. insert vs tombstone)
		 */
		public QuadEntry(long subject, long predicate, long object, long context, byte flag) {
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
			this.context = context;
			this.flag = flag;
		}
	}

	/**
	 * Builds a Parquet file from the given entries using default settings.
	 *
	 * <p>
	 * Uses {@link ParquetSchemas#PARTITIONED_SCHEMA}, 8 MiB row group size, and 64 KiB page size.
	 *
	 * @param entries   the quad entries to write (must already be sorted)
	 * @param sortOrder the sort order of the entries
	 * @return the serialized Parquet file as a byte array
	 */
	public static byte[] build(List<QuadEntry> entries, ParquetSchemas.SortOrder sortOrder) {
		return build(entries, ParquetSchemas.PARTITIONED_SCHEMA, sortOrder, -1,
				DEFAULT_ROW_GROUP_SIZE, DEFAULT_PAGE_SIZE);
	}

	/**
	 * Builds a Parquet file from the given entries with full control over parameters.
	 *
	 * @param entries      the quad entries to write (must already be sorted)
	 * @param schema       the Parquet schema to use
	 * @param sortOrder    the sort order of the entries
	 * @param predicateId  the predicate ID for partitioned files (ignored for unpartitioned)
	 * @param rowGroupSize the row group size in bytes
	 * @param pageSize     the page size in bytes
	 * @return the serialized Parquet file as a byte array
	 */
	public static byte[] build(List<QuadEntry> entries, MessageType schema,
			ParquetSchemas.SortOrder sortOrder, long predicateId,
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
		private final boolean hasPredicateColumn;
		private RecordConsumer recordConsumer;

		QuadEntryWriteSupport(MessageType schema) {
			this.schema = schema;
			this.hasPredicateColumn = schema.containsField(ParquetSchemas.COL_PREDICATE);
		}

		@Override
		public WriteContext init(org.apache.hadoop.conf.Configuration configuration) {
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

			// predicate (only for unpartitioned schema)
			if (hasPredicateColumn) {
				recordConsumer.startField(ParquetSchemas.COL_PREDICATE, fieldIndex);
				recordConsumer.addLong(entry.predicate);
				recordConsumer.endField(ParquetSchemas.COL_PREDICATE, fieldIndex);
				fieldIndex++;
			}

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
		protected WriteSupport<QuadEntry> getWriteSupport(
				org.apache.hadoop.conf.Configuration conf) {
			return new QuadEntryWriteSupport(schema);
		}

		@Override
		protected WriteSupport<QuadEntry> getWriteSupport(ParquetConfiguration conf) {
			return new QuadEntryWriteSupport(schema);
		}
	}
}
