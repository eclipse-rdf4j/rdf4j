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
import java.util.EnumSet;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DualUnionIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;

/**
 * LMDB-aware union dataset that exposes ID-level access when both sides provide it, with a conservative fallback.
 */
final class LmdbUnionSailDataset implements SailDataset, LmdbEvaluationDataset {

	private final SailDataset dataset1;
	private final SailDataset dataset2;
	private final ValueStore valueStore;

	LmdbUnionSailDataset(SailDataset dataset1, SailDataset dataset2, ValueStore valueStore) {
		this.dataset1 = dataset1;
		this.dataset2 = dataset2;
		this.valueStore = valueStore;
	}

	@Override
	public void close() throws SailException {
		try {
			dataset1.close();
		} finally {
			dataset2.close();
		}
	}

	@Override
	public CloseableIteration<? extends Namespace> getNamespaces() throws SailException {
		return DualUnionIteration.getWildcardInstance(dataset1.getNamespaces(), dataset2.getNamespaces());
	}

	@Override
	public String getNamespace(String prefix) throws SailException {
		String ns = dataset1.getNamespace(prefix);
		return ns != null ? ns : dataset2.getNamespace(prefix);
	}

	@Override
	public CloseableIteration<? extends Resource> getContextIDs() throws SailException {
		return DualUnionIteration.getWildcardInstance(dataset1.getContextIDs(), dataset2.getContextIDs());
	}

	@Override
	public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
			Resource... contexts) throws SailException {
		return DualUnionIteration.getWildcardInstance(dataset1.getStatements(subj, pred, obj, contexts),
				dataset2.getStatements(subj, pred, obj, contexts));
	}

	@Override
	public CloseableIteration<? extends Triple> getTriples(Resource subj, IRI pred, Value obj) throws SailException {
		return DualUnionIteration.getWildcardInstance(dataset1.getTriples(subj, pred, obj),
				dataset2.getTriples(subj, pred, obj));
	}

	@Override
	public CloseableIteration<? extends Statement> getStatements(StatementOrder statementOrder, Resource subj, IRI pred,
			Value obj, Resource... contexts) throws SailException {
		CloseableIteration<? extends Statement> it1 = dataset1.getStatements(statementOrder, subj, pred, obj, contexts);
		CloseableIteration<? extends Statement> it2 = dataset2.getStatements(statementOrder, subj, pred, obj, contexts);
		Comparator<Value> cmp = dataset1.getComparator();
		assert cmp != null && dataset2.getComparator() != null;
		return DualUnionIteration.getWildcardInstance(statementOrder.getComparator(cmp), it1, it2);
	}

	@Override
	public Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts) {
		Set<StatementOrder> s1 = dataset1.getSupportedOrders(subj, pred, obj, contexts);
		if (s1.isEmpty())
			return Set.of();
		Set<StatementOrder> s2 = dataset2.getSupportedOrders(subj, pred, obj, contexts);
		if (s2.isEmpty())
			return Set.of();
		if (s1.equals(s2))
			return s1;
		EnumSet<StatementOrder> common = EnumSet.copyOf(s1);
		common.retainAll(s2);
		return common;
	}

	@Override
	public Comparator<Value> getComparator() {
		Comparator<Value> c1 = dataset1.getComparator();
		Comparator<Value> c2 = dataset2.getComparator();
		if (c1 == null || c2 == null)
			return null;
		return c1;
	}

	@Override
	public RecordIterator getRecordIterator(org.eclipse.rdf4j.query.algebra.StatementPattern pattern,
			org.eclipse.rdf4j.query.BindingSet bindings) throws org.eclipse.rdf4j.query.QueryEvaluationException {
		boolean d1 = dataset1 instanceof LmdbEvaluationDataset;
		boolean d2 = dataset2 instanceof LmdbEvaluationDataset;
		if (d1 && d2) {
			RecordIterator r1 = ((LmdbEvaluationDataset) dataset1).getRecordIterator(pattern, bindings);
			RecordIterator r2 = ((LmdbEvaluationDataset) dataset2).getRecordIterator(pattern, bindings);
			return chain(r1, r2);
		}
		LmdbDelegatingSailDataset a = new LmdbDelegatingSailDataset(dataset1, valueStore);
		LmdbDelegatingSailDataset b = new LmdbDelegatingSailDataset(dataset2, valueStore);
		return chain(a.getRecordIterator(pattern, bindings), b.getRecordIterator(pattern, bindings));
	}

	@Override
	public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds) throws org.eclipse.rdf4j.query.QueryEvaluationException {
		return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, null);
	}

	@Override
	public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds, long[] reuse) throws org.eclipse.rdf4j.query.QueryEvaluationException {
		boolean d1 = dataset1 instanceof LmdbEvaluationDataset;
		boolean d2 = dataset2 instanceof LmdbEvaluationDataset;
		if (d1 && d2) {
			RecordIterator r1 = ((LmdbEvaluationDataset) dataset1).getRecordIterator(binding, subjIndex, predIndex,
					objIndex, ctxIndex, patternIds, reuse);
			RecordIterator r2 = ((LmdbEvaluationDataset) dataset2).getRecordIterator(binding, subjIndex, predIndex,
					objIndex, ctxIndex, patternIds, reuse);
			return chain(r1, r2);
		}
		LmdbDelegatingSailDataset a = new LmdbDelegatingSailDataset(dataset1, valueStore);
		LmdbDelegatingSailDataset b = new LmdbDelegatingSailDataset(dataset2, valueStore);
		return chain(a.getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, reuse),
				b.getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, reuse));
	}

	@Override
	public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order)
			throws org.eclipse.rdf4j.query.QueryEvaluationException {
		return getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, order, null);
	}

	@Override
	public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order, long[] reuse)
			throws org.eclipse.rdf4j.query.QueryEvaluationException {
		boolean d1 = dataset1 instanceof LmdbEvaluationDataset;
		boolean d2 = dataset2 instanceof LmdbEvaluationDataset;
		if (d1 && d2) {
			RecordIterator r1 = ((LmdbEvaluationDataset) dataset1).getOrderedRecordIterator(binding, subjIndex,
					predIndex, objIndex, ctxIndex, patternIds, order, reuse);
			RecordIterator r2 = ((LmdbEvaluationDataset) dataset2).getOrderedRecordIterator(binding, subjIndex,
					predIndex, objIndex, ctxIndex, patternIds, order, reuse);
			return chain(r1, r2);
		}
		return LmdbEvaluationDataset.super.getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex,
				patternIds, order, reuse);
	}

	@Override
	public ValueStore getValueStore() {
		return valueStore;
	}

	private RecordIterator chain(RecordIterator left, RecordIterator right) {
		return new RecordIterator() {
			boolean leftDone = false;

			@Override
			public long[] next() throws org.eclipse.rdf4j.query.QueryEvaluationException {
				if (!leftDone) {
					long[] n = left.next();
					if (n != null)
						return n;
					leftDone = true;
				}
				return right.next();
			}

			@Override
			public void close() {
				try {
					left.close();
				} finally {
					right.close();
				}
			}
		};
	}
}
