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
	 * Create a {@link RecordIterator} for the supplied {@link StatementPattern}, taking into account any existing
	 * variable bindings provided as LMDB internal IDs.
	 *
	 * <p>
	 * This overload avoids materializing Values during join evaluation and should be used when chaining joins in an
	 * ID-only pipeline.
	 * </p>
	 *
	 * @param pattern   the statement pattern to evaluate
	 * @param idBinding a binding accessor that can provide internal IDs for variables present on the left side of the
	 *                  join
	 * @return a {@link RecordIterator} that yields matching statement records
	 * @throws QueryEvaluationException if the iterator could not be created
	 */
	@InternalUseOnly
	public RecordIterator getRecordIterator(StatementPattern pattern, LmdbIdVarBinding idBinding)
			throws QueryEvaluationException;

	/**
	 * @return the {@link ValueStore} backing this dataset.
	 */
	ValueStore getValueStore();

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
