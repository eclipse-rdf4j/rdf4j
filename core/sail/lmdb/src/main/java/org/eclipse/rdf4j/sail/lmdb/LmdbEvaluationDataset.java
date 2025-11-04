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
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

/**
 * Provides LMDB-specific access needed during query evaluation.
 */
public interface LmdbEvaluationDataset {

	/**
	 * Create a {@link RecordIterator} for the supplied {@link StatementPattern}, taking into account any existing
	 * bindings.
	 *
	 * @param pattern  the statement pattern to evaluate
	 * @param bindings the bindings that should be respected when creating the iterator
	 * @return a {@link RecordIterator} that yields matching statement records
	 * @throws QueryEvaluationException if the iterator could not be created
	 */
	RecordIterator getRecordIterator(StatementPattern pattern, BindingSet bindings) throws QueryEvaluationException;

	/**
	 * Create a {@link RecordIterator} for the supplied pattern, expressed as internal IDs, using the current binding
	 * snapshot.
	 *
	 * <p>
	 * The {@code binding} array represents the accumulated variable IDs for the join so far and must be treated as
	 * read-only. The {@code patternIds} array contains four entries (subject, predicate, object, context) where a value
	 * of {@link org.eclipse.rdf4j.sail.lmdb.model.LmdbValue#UNKNOWN_ID} indicates that the corresponding position is
	 * unbound in the pattern. The index arguments point to the slots in {@code binding} where the resolved IDs should
	 * be written (or {@code -1} if that pattern position does not correspond to a variable). Implementations may reuse
	 * internal buffers by copying {@code binding} into a scratch array before mutating; callers can supply such an
	 * array via {@link #getRecordIterator(long[], int, int, int, int, long[], long[])} to avoid per-iterator
	 * allocation.
	 * </p>
	 *
	 * @param binding    the current binding snapshot; implementations must copy before mutating
	 * @param subjIndex  index in {@code binding} for the subject variable, or {@code -1} if none
	 * @param predIndex  index in {@code binding} for the predicate variable, or {@code -1} if none
	 * @param objIndex   index in {@code binding} for the object variable, or {@code -1} if none
	 * @param ctxIndex   index in {@code binding} for the context variable, or {@code -1} if none
	 * @param patternIds pattern constants for subject/predicate/object/context
	 * @return a {@link RecordIterator} that yields binding snapshots with the pattern applied
	 * @throws QueryEvaluationException if the iterator could not be created
	 */
	@InternalUseOnly
	RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds) throws QueryEvaluationException;

	/**
	 * Variant of {@link #getRecordIterator(long[], int, int, int, int, long[])} that allows callers to supply a
	 * reusable scratch buffer. Implementations should treat {@code binding} as read-only and (when {@code reuse} is
	 * non-null and large enough) seed the scratch buffer with the binding state before producing rows from this
	 * iterator.
	 *
	 * @param reuse optional scratch buffer that may be mutated and returned from {@link RecordIterator#next()}
	 */
	@InternalUseOnly
	default RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds, long[] reuse) throws QueryEvaluationException {
		return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds);
	}

	/**
	 * Create an ordered {@link RecordIterator} for the supplied pattern expressed via internal IDs and binding indexes.
	 * Implementations may fall back to the unordered iterator when the requested order is unsupported.
	 */
	@InternalUseOnly
	default RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order) throws QueryEvaluationException {
		if (order == null) {
			return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds);
		}
		return null;
	}

	/**
	 * Variant of {@link #getOrderedRecordIterator(long[], int, int, int, int, long[], StatementOrder)} that accepts a
	 * reusable scratch buffer.
	 */
	@InternalUseOnly
	default RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order, long[] reuse) throws QueryEvaluationException {
		if (order == null) {
			return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, reuse);
		}
		return null;
	}

	/**
	 * Create an ordered {@link RecordIterator} for the supplied pattern. Implementations may fall back to the unordered
	 * iterator when the requested order is unsupported.
	 */
	default RecordIterator getOrderedRecordIterator(StatementPattern pattern, BindingSet bindings, StatementOrder order)
			throws QueryEvaluationException {
		if (order == null) {
			return getRecordIterator(pattern, bindings);
		}
		return null;
	}

	default boolean supportsOrder(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds, StatementOrder order) {
		return order == null;
	}

	/**
	 * @return the {@link ValueStore} backing this dataset.
	 */
	ValueStore getValueStore();

	/**
	 * @return the isolation level associated with this dataset.
	 */
	default IsolationLevel getIsolationLevel() {
		return null;
	}

	/**
	 * Refresh the underlying snapshot, if applicable, to ensure subsequent reads observe the latest committed data.
	 * Implementations that do not maintain a snapshot may ignore this call.
	 */
	default void refreshSnapshot() throws QueryEvaluationException {
		// no-op by default
	}

	/**
	 * Indicates whether the current evaluation should consider the active transaction as containing uncommitted changes
	 * that require reading through an overlay rather than directly from the LMDB indexes.
	 *
	 * <p>
	 * Implementations that expose a transaction overlay should override this to return {@code true}. The default is
	 * {@code false} for plain snapshot datasets.
	 * </p>
	 *
	 * @return {@code true} if the evaluation is layered over uncommitted transaction changes; {@code false} otherwise.
	 */
	default boolean hasTransactionChanges() {
		return false;
	}
}
