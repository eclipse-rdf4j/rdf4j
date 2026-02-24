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

import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Types;

/**
 * Parquet schema definitions for quad storage.
 *
 * <p>
 * Two schemas are provided:
 * <ul>
 * <li>{@link #PARTITIONED_SCHEMA} - for files within {@code predicates/{id}/} directories, where the predicate is
 * implicit in the partition path.</li>
 * <li>{@link #UNPARTITIONED_SCHEMA} - for files in {@code _unpartitioned/}, which include an explicit predicate
 * column.</li>
 * </ul>
 */
public final class ParquetSchemas {

	/** Column name for subject ID. */
	public static final String COL_SUBJECT = "subject";

	/** Column name for predicate ID. */
	public static final String COL_PREDICATE = "predicate";

	/** Column name for object ID. */
	public static final String COL_OBJECT = "object";

	/** Column name for context (named graph) ID. */
	public static final String COL_CONTEXT = "context";

	/** Column name for the entry flag (e.g. insert vs tombstone). */
	public static final String COL_FLAG = "flag";

	/**
	 * Schema for partitioned Parquet files stored under {@code predicates/{id}/}. The predicate is implicit in the
	 * directory path and not stored as a column.
	 */
	public static final MessageType PARTITIONED_SCHEMA = Types.buildMessage()
			.required(PrimitiveTypeName.INT64)
			.named(COL_SUBJECT)
			.required(PrimitiveTypeName.INT64)
			.named(COL_OBJECT)
			.required(PrimitiveTypeName.INT64)
			.named(COL_CONTEXT)
			.required(PrimitiveTypeName.INT32)
			.named(COL_FLAG)
			.named("quad_partitioned");

	/**
	 * Schema for unpartitioned Parquet files stored under {@code _unpartitioned/}. Includes an explicit predicate
	 * column.
	 */
	public static final MessageType UNPARTITIONED_SCHEMA = Types.buildMessage()
			.required(PrimitiveTypeName.INT64)
			.named(COL_SUBJECT)
			.required(PrimitiveTypeName.INT64)
			.named(COL_PREDICATE)
			.required(PrimitiveTypeName.INT64)
			.named(COL_OBJECT)
			.required(PrimitiveTypeName.INT64)
			.named(COL_CONTEXT)
			.required(PrimitiveTypeName.INT32)
			.named(COL_FLAG)
			.named("quad_unpartitioned");

	/**
	 * Sort orders for quad entries within a Parquet file.
	 */
	public enum SortOrder {
		/** Subject-Object-Context ordering (partitioned). */
		SOC("soc"),
		/** Object-Subject-Context ordering (partitioned). */
		OSC("osc"),
		/** Context-Subject-Object ordering (partitioned). */
		CSO("cso"),
		/** Subject-Predicate-Object-Context ordering (unpartitioned). */
		SPOC("spoc");

		private final String suffix;

		SortOrder(String suffix) {
			this.suffix = suffix;
		}

		/**
		 * Returns the file-name suffix for this sort order.
		 *
		 * @return the suffix string
		 */
		public String suffix() {
			return suffix;
		}

		/**
		 * Returns the SortOrder for the given suffix string.
		 *
		 * @param suffix the suffix (e.g. "soc", "osc", "cso", "spoc")
		 * @return the matching SortOrder
		 * @throws IllegalArgumentException if no match found
		 */
		public static SortOrder fromSuffix(String suffix) {
			for (SortOrder so : values()) {
				if (so.suffix.equals(suffix)) {
					return so;
				}
			}
			throw new IllegalArgumentException("Unknown sort order suffix: " + suffix);
		}
	}

	private ParquetSchemas() {
		// utility class
	}
}
