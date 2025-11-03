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

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.sail.TripleSourceIterationWrapper;
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdJoinIterator;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * Adapter that adds ID-level access to an arbitrary TripleSource by resolving IDs via the provided ValueStore. This
 * does not avoid materialization when the underlying TripleSource cannot serve IDs directly; it centralizes the
 * fallback for overlay scenarios.
 */
final class LmdbIdTripleSourceAdapter implements TripleSource, LmdbIdTripleSource {

	private final TripleSource delegate;
	private final ValueStore valueStore;

	LmdbIdTripleSourceAdapter(TripleSource delegate, ValueStore valueStore) {
		this.delegate = delegate;
		this.valueStore = valueStore;
	}

	// TripleSource delegation
	@Override
	public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
			Resource... contexts) throws QueryEvaluationException {
		CloseableIteration<? extends Statement> statements = delegate.getStatements(subj, pred, obj, contexts);
		if (statements instanceof EmptyIteration) {
			return statements;
		}
		return new TripleSourceIterationWrapper<>(statements);
	}

	@Override
	@Experimental
	public CloseableIteration<? extends Statement> getStatements(org.eclipse.rdf4j.common.order.StatementOrder order,
			Resource subj, IRI pred, Value obj, Resource... contexts) throws QueryEvaluationException {
		return delegate.getStatements(order, subj, pred, obj, contexts);
	}

	@Override
	@Experimental
	public Set<org.eclipse.rdf4j.common.order.StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj,
			Resource... contexts) {
		return delegate.getSupportedOrders(subj, pred, obj, contexts);
	}

	@Override
	@Experimental
	public Comparator<Value> getComparator() {
		return delegate.getComparator();
	}

	@Override
	public ValueFactory getValueFactory() {
		return delegate.getValueFactory();
	}

	// ID-level implementation
	@Override
	public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds) throws QueryEvaluationException {
		// Prefer direct ID-level access if the delegate already supports it
		if (delegate instanceof LmdbIdTripleSource) {
			return ((LmdbIdTripleSource) delegate).getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex,
					patternIds);
		}

		// If no active connection changes, delegate to the current LMDB dataset to avoid materialization
		if (!LmdbEvaluationStrategy.hasActiveConnectionChanges()) {
			var dsOpt = LmdbEvaluationStrategy.getCurrentDataset();
			if (dsOpt.isPresent()) {
				return dsOpt.get().getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds);
			}
		}

		// Fallback: materializing path via delegate TripleSource (only when unavoidable)
		Value subjValue = valueForQuery(patternIds[TripleStore.SUBJ_IDX], binding, subjIndex, true, false);
		Resource subjRes = subjValue == null ? null : (Resource) subjValue;
		Value predValue = valueForQuery(patternIds[TripleStore.PRED_IDX], binding, predIndex, false, true);
		IRI predIri = predValue == null ? null : (IRI) predValue;
		Value objValue = valueForQuery(patternIds[TripleStore.OBJ_IDX], binding, objIndex, false, false);
		long ctxQueryId = selectQueryId(patternIds[TripleStore.CONTEXT_IDX], binding, ctxIndex);
		boolean requireDefaultContext = ctxQueryId == 0;
		Resource[] contexts;
		if (ctxQueryId > 0) {
			try {
				Value ctxValue = valueStore.getLazyValue(ctxQueryId);
				if (!(ctxValue instanceof Resource) || ((Resource) ctxValue).isTriple()) {
					return LmdbIdJoinIterator.emptyRecordIterator();
				}
				contexts = new Resource[] { (Resource) ctxValue };
			} catch (Exception e) {
				throw new QueryEvaluationException(e);
			}
		} else {
			contexts = new Resource[0];
		}

		CloseableIteration<? extends Statement> stmts = contexts.length == 0
				? delegate.getStatements(subjRes, predIri, objValue)
				: delegate.getStatements(subjRes, predIri, objValue, contexts);

		final boolean defaultOnly = requireDefaultContext;
		return new RecordIterator() {
			@Override
			public long[] next() throws QueryEvaluationException {
				while (true) {
					try {
						if (!stmts.hasNext()) {
							stmts.close();
							return null;
						}
						Statement st = stmts.next();
						if (defaultOnly && st.getContext() != null) {
							continue;
						}
						long subjId = resolveId(st.getSubject());
						long predId = resolveId(st.getPredicate());
						long objId = resolveId(st.getObject());
						long ctxId = st.getContext() == null ? 0L : resolveId(st.getContext());
						long[] merged = mergeBinding(binding, subjId, predId, objId, ctxId, subjIndex, predIndex,
								objIndex, ctxIndex);
						if (merged != null) {
							return merged;
						}
					} catch (QueryEvaluationException e) {
						throw e;
					} catch (Exception e) {
						try {
							stmts.close();
						} catch (Exception ignore) {
						}
						throw new QueryEvaluationException(e);
					}
				}
			}

			@Override
			public void close() {
				try {
					stmts.close();
				} catch (Exception ignore) {
				}
			}
		};
	}

	private long selectQueryId(long patternId, long[] binding, int index) {
		if (patternId != LmdbValue.UNKNOWN_ID) {
			return patternId;
		}
		if (index >= 0 && index < binding.length) {
			return binding[index];
		}
		return LmdbValue.UNKNOWN_ID;
	}

	private Value valueForQuery(long patternId, long[] binding, int index, boolean requireResource, boolean requireIri)
			throws QueryEvaluationException {
		long id = selectQueryId(patternId, binding, index);
		if (id == LmdbValue.UNKNOWN_ID) {
			return null;
		}
		try {
			Value value = valueStore.getLazyValue(id);
			if (requireResource && !(value instanceof Resource)) {
				throw new QueryEvaluationException("Expected resource-bound value");
			}
			if (requireIri && !(value instanceof IRI)) {
				throw new QueryEvaluationException("Expected IRI-bound value");
			}
			if (value instanceof Resource && value.isTriple()) {
				throw new QueryEvaluationException("Triple-valued resources are not supported in LMDB joins");
			}
			return value;
		} catch (Exception e) {
			throw e instanceof QueryEvaluationException ? (QueryEvaluationException) e
					: new QueryEvaluationException(e);
		}
	}

	private long[] mergeBinding(long[] binding, long subjId, long predId, long objId, long ctxId, int subjIndex,
			int predIndex, int objIndex, int ctxIndex) {
		long[] out = java.util.Arrays.copyOf(binding, binding.length);
		if (!applyValue(out, subjIndex, subjId))
			return null;
		if (!applyValue(out, predIndex, predId))
			return null;
		if (!applyValue(out, objIndex, objId))
			return null;
		if (!applyValue(out, ctxIndex, ctxId))
			return null;
		return out;
	}

	private boolean applyValue(long[] target, int index, long value) {
		if (index < 0)
			return true;
		long existing = target[index];
		if (existing != LmdbValue.UNKNOWN_ID && existing != value)
			return false;
		target[index] = value;
		return true;
	}

	private long resolveId(Value value) throws Exception {
		if (value == null) {
			return LmdbValue.UNKNOWN_ID;
		}
		if (value instanceof LmdbValue) {
			LmdbValue lmdb = (LmdbValue) value;
			if (valueStore.getRevision().equals(lmdb.getValueStoreRevision())) {
				return lmdb.getInternalID();
			}
		}
		return valueStore.getId(value);
	}
}
