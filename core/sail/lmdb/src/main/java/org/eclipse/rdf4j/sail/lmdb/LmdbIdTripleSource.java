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
package org.eclipse.rdf4j.sail.lmdb;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * LMDB extension of TripleSource that supports ID-level statement access without materializing RDF4J Value objects.
 */
@InternalUseOnly
public interface LmdbIdTripleSource {

	/**
	 * Create an iterator over ID-level bindings for the given pattern and binding snapshot.
	 *
	 * @param binding    current binding snapshot (read-only for implementations)
	 * @param subjIndex  index in binding for subject var, or -1
	 * @param predIndex  index in binding for predicate var, or -1
	 * @param objIndex   index in binding for object var, or -1
	 * @param ctxIndex   index in binding for context var, or -1
	 * @param patternIds constants for S/P/O/C positions (UNKNOWN_ID for wildcard)
	 */
	RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds) throws QueryEvaluationException;

	/**
	 * Variant that accepts a reusable scratch buffer.
	 */
	default RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds, long[] reuse) throws QueryEvaluationException {
		return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds);
	}

	/**
	 * Create an ordered iterator over ID-level bindings; may fall back to the unordered iterator if unsupported.
	 */
	default RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order) throws QueryEvaluationException {
		if (order == null) {
			return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds);
		}
		return null;
	}

	/**
	 * Variant that accepts a reusable scratch buffer.
	 */
	default RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order, long[] reuse) throws QueryEvaluationException {
		if (order == null) {
			return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, reuse);
		}
		return null;
	}
}
