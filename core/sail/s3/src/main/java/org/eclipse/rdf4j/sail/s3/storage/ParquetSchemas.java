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
 * All files use {@link #QUAD_SCHEMA} with 5 columns (subject, predicate, object, context, flag). Three sort orders
 * determine the key encoding: SPOC (subject-leading), OPSC (object-leading), and CSPO (context-leading).
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
	 * Schema for all Parquet files. Includes all 5 columns: subject, predicate, object, context, flag.
	 */
	public static final MessageType QUAD_SCHEMA = Types.buildMessage()
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
			.named("quad");

	/**
	 * Sort orders for quad entries within a Parquet file.
	 */
	public enum SortOrder {
		/** Subject-Predicate-Object-Context ordering. */
		SPOC("spoc"),
		/** Object-Predicate-Subject-Context ordering. */
		OPSC("opsc"),
		/** Context-Subject-Predicate-Object ordering. */
		CSPO("cspo");

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
		 * @param suffix the suffix (e.g. "spoc", "opsc", "cspo")
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
