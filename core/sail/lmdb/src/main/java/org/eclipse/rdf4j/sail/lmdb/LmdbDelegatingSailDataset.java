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

import java.util.Comparator;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;

/**
 * LMDB-aware delegating dataset that forwards ID-level calls when the delegate supports them, otherwise conservatively
 * falls back to value-based conversion using a ValueStore.
 */
final class LmdbDelegatingSailDataset implements SailDataset, LmdbEvaluationDataset {

	private final SailDataset delegate;
	private final ValueStore valueStore;

	LmdbDelegatingSailDataset(SailDataset delegate, ValueStore valueStore) {
		this.delegate = delegate;
		this.valueStore = valueStore;
	}

	@Override
	public void close() throws SailException {
		delegate.close();
	}

	@Override
	public CloseableIteration<? extends Namespace> getNamespaces() throws SailException {
		return delegate.getNamespaces();
	}

	@Override
	public String getNamespace(String prefix) throws SailException {
		return delegate.getNamespace(prefix);
	}

	@Override
	public CloseableIteration<? extends Resource> getContextIDs() throws SailException {
		return delegate.getContextIDs();
	}

	@Override
	public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
			Resource... contexts) throws SailException {
		return delegate.getStatements(subj, pred, obj, contexts);
	}

	@Override
	public CloseableIteration<? extends Statement> getStatements(StatementOrder statementOrder, Resource subj, IRI pred,
			Value obj, Resource... contexts) throws SailException {
		return delegate.getStatements(statementOrder, subj, pred, obj, contexts);
	}

	@Override
	public Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return delegate.getSupportedOrders(subj, pred, obj, contexts);
	}

	@Override
	public Comparator<Value> getComparator() {
		return delegate.getComparator();
	}

	@Override
	public RecordIterator getRecordIterator(org.eclipse.rdf4j.query.algebra.StatementPattern pattern,
			org.eclipse.rdf4j.query.BindingSet bindings) throws QueryEvaluationException {
		if (delegate instanceof LmdbEvaluationDataset) {
			return ((LmdbEvaluationDataset) delegate).getRecordIterator(pattern, bindings);
		}
		// Fallback via overlay helper using ValueStore (conservative)
		LmdbOverlayEvaluationDataset helper = new LmdbOverlayEvaluationDataset(
				new LmdbSailDatasetTripleSource(valueStore, delegate), valueStore);
		return helper.getRecordIterator(pattern, bindings);
	}

	@Override
	public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds) throws QueryEvaluationException {
		return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, null, null);
	}

	@Override
	public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds, long[] reuse) throws QueryEvaluationException {
		return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, reuse, null);
	}

	@Override
	public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds, long[] reuse, long[] quadReuse) throws QueryEvaluationException {
		if (delegate instanceof LmdbEvaluationDataset) {
			return ((LmdbEvaluationDataset) delegate).getRecordIterator(binding, subjIndex, predIndex, objIndex,
					ctxIndex, patternIds, reuse, quadReuse);
		}
		// Fallback via TripleSource with Value conversion
		return new LmdbSailDatasetTripleSource(valueStore, delegate)
				.getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, reuse, quadReuse);
	}

	@Override
	public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order) throws QueryEvaluationException {
		return getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, order, null,
				null);
	}

	@Override
	public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order, long[] reuse) throws QueryEvaluationException {
		return getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, order, reuse,
				null);
	}

	@Override
	public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order, long[] reuse, long[] quadReuse)
			throws QueryEvaluationException {
		if (delegate instanceof LmdbEvaluationDataset) {
			return ((LmdbEvaluationDataset) delegate).getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex,
					ctxIndex, patternIds, order, reuse, quadReuse);
		}
		return LmdbEvaluationDataset.super.getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex,
				patternIds, order, reuse, quadReuse);
	}

	@Override
	public ValueStore getValueStore() {
		return valueStore;
	}
}
