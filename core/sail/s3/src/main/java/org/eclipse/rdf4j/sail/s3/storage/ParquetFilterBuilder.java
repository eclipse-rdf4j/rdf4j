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

import static org.apache.parquet.filter2.predicate.FilterApi.and;
import static org.apache.parquet.filter2.predicate.FilterApi.eq;
import static org.apache.parquet.filter2.predicate.FilterApi.longColumn;

import org.apache.parquet.filter2.compat.FilterCompat;
import org.apache.parquet.filter2.predicate.FilterPredicate;

/**
 * Builds Parquet {@link FilterPredicate}s from quad query patterns. Bound components (>= 0) become equality filters;
 * unbound components (-1) are omitted.
 */
public class ParquetFilterBuilder {

	/**
	 * Builds a Parquet filter for a within-partition query (predicate is implicit).
	 *
	 * @param subject subject ID, or -1 for wildcard
	 * @param object  object ID, or -1 for wildcard
	 * @param context context ID, or -1 for wildcard
	 * @return a FilterCompat.Filter, or FilterCompat.NOOP if no filters apply
	 */
	public static FilterCompat.Filter buildPartitionedFilter(long subject, long object, long context) {
		FilterPredicate predicate = null;

		if (subject >= 0) {
			predicate = eq(longColumn(ParquetSchemas.COL_SUBJECT), subject);
		}
		if (object >= 0) {
			FilterPredicate objFilter = eq(longColumn(ParquetSchemas.COL_OBJECT), object);
			predicate = predicate != null ? and(predicate, objFilter) : objFilter;
		}
		if (context >= 0) {
			FilterPredicate ctxFilter = eq(longColumn(ParquetSchemas.COL_CONTEXT), context);
			predicate = predicate != null ? and(predicate, ctxFilter) : ctxFilter;
		}

		return predicate != null ? FilterCompat.get(predicate) : FilterCompat.NOOP;
	}

	/**
	 * Builds a Parquet filter for an unpartitioned file query (all 4 components).
	 *
	 * @param subject subject ID, or -1 for wildcard
	 * @param predId  predicate ID, or -1 for wildcard
	 * @param object  object ID, or -1 for wildcard
	 * @param context context ID, or -1 for wildcard
	 * @return a FilterCompat.Filter, or FilterCompat.NOOP if no filters apply
	 */
	public static FilterCompat.Filter buildUnpartitionedFilter(long subject, long predId, long object, long context) {
		FilterPredicate predicate = null;

		if (subject >= 0) {
			predicate = eq(longColumn(ParquetSchemas.COL_SUBJECT), subject);
		}
		if (predId >= 0) {
			FilterPredicate pFilter = eq(longColumn(ParquetSchemas.COL_PREDICATE), predId);
			predicate = predicate != null ? and(predicate, pFilter) : pFilter;
		}
		if (object >= 0) {
			FilterPredicate objFilter = eq(longColumn(ParquetSchemas.COL_OBJECT), object);
			predicate = predicate != null ? and(predicate, objFilter) : objFilter;
		}
		if (context >= 0) {
			FilterPredicate ctxFilter = eq(longColumn(ParquetSchemas.COL_CONTEXT), context);
			predicate = predicate != null ? and(predicate, ctxFilter) : ctxFilter;
		}

		return predicate != null ? FilterCompat.get(predicate) : FilterCompat.NOOP;
	}
}
