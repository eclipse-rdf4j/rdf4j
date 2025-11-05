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

import java.util.Arrays;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdJoinIterator;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * Delegates reads via the Sail {@link TripleSource} to honor transaction overlays and isolation while exposing
 * LMDB-style {@link RecordIterator}s for ID-based joins.
 */
final class LmdbOverlayEvaluationDataset implements LmdbEvaluationDataset {

	private final TripleSource tripleSource;
	private final ValueStore valueStore;

	LmdbOverlayEvaluationDataset(TripleSource tripleSource, ValueStore valueStore) {
		this.tripleSource = tripleSource;
		this.valueStore = valueStore;
	}

	@Override
	public RecordIterator getRecordIterator(StatementPattern pattern, BindingSet bindings)
			throws QueryEvaluationException {
		return getRecordIteratorInternal(pattern, bindings, null);
	}

	@Override
	public RecordIterator getRecordIterator(StatementPattern pattern, BindingSet bindings, KeyRangeBuffers keyBuffers)
			throws QueryEvaluationException {
		return getRecordIteratorInternal(pattern, bindings, keyBuffers);
	}

	private RecordIterator getRecordIteratorInternal(StatementPattern pattern, BindingSet bindings,
			KeyRangeBuffers keyBuffers)
			throws QueryEvaluationException {

		Value subj = resolveValue(pattern.getSubjectVar(), bindings);
		if (subj != null && !(subj instanceof Resource)) {
			return LmdbIdJoinIterator.emptyRecordIterator();
		}
		Value pred = resolveValue(pattern.getPredicateVar(), bindings);
		if (pred != null && !(pred instanceof IRI)) {
			return LmdbIdJoinIterator.emptyRecordIterator();
		}
		Value obj = resolveValue(pattern.getObjectVar(), bindings);
		Value ctxVal = resolveValue(pattern.getContextVar(), bindings);

		Resource subjRes = (Resource) subj; // may be null
		IRI predIri = (IRI) pred; // may be null

		if (ctxVal != null && !(ctxVal instanceof Resource)) {
			return LmdbIdJoinIterator.emptyRecordIterator();
		}
		if (ctxVal != null && ctxVal.isTriple()) {
			return LmdbIdJoinIterator.emptyRecordIterator();
		}

		Resource[] contexts;
		if (ctxVal == null) {
			contexts = new Resource[0];
		} else {
			contexts = new Resource[] { (Resource) ctxVal };
		}

		final CloseableIteration<? extends Statement> stmts = contexts.length == 0
				? tripleSource.getStatements(subjRes, predIri, obj)
				: tripleSource.getStatements(subjRes, predIri, obj, contexts);

		return new RecordIterator() {
			@Override
			public long[] next() throws QueryEvaluationException {
				try {
					if (!stmts.hasNext()) {
						stmts.close();
						return null;
					}
					Statement st = stmts.next();
					if (st == null) {
						stmts.close();
						return null;
					}
					long s = resolveId(st.getSubject());
					long p = resolveId(st.getPredicate());
					long o = resolveId(st.getObject());
					long c = st.getContext() == null ? 0L : resolveId(st.getContext());
					return new long[] { s, p, o, c };
				} catch (Exception e) {
					try {
						stmts.close();
					} catch (Exception ignore) {
					}
					if (e instanceof QueryEvaluationException) {
						throw (QueryEvaluationException) e;
					}
					throw new QueryEvaluationException(e);
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

	@Override
	public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds) throws QueryEvaluationException {
		return getRecordIteratorInternal(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, null, null,
				null);
	}

	@Override
	public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds, KeyRangeBuffers keyBuffers) throws QueryEvaluationException {
		return getRecordIteratorInternal(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, keyBuffers,
				null, null);
	}

	@Override
	public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds, long[] reuse) throws QueryEvaluationException {
		return getRecordIteratorInternal(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, null, reuse,
				null);
	}

	@Override
	public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds, long[] reuse, long[] quadReuse) throws QueryEvaluationException {
		return getRecordIteratorInternal(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, null, reuse,
				quadReuse);
	}

	@Override
	public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds, KeyRangeBuffers keyBuffers, long[] reuse, long[] quadReuse)
			throws QueryEvaluationException {
		return getRecordIteratorInternal(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, keyBuffers,
				reuse, quadReuse);
	}

	private RecordIterator getRecordIteratorInternal(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, KeyRangeBuffers keyBuffers, long[] reuse, long[] quadReuse)
			throws QueryEvaluationException {
		// Prefer an ID-level path if the TripleSource supports it and we can trust overlay correctness.
		if (tripleSource instanceof LmdbIdTripleSource) {
			// The overlay dataset is represented by this LmdbOverlayEvaluationDataset; the tripleSource reflects the
			// current branch dataset state (including transaction overlays). Therefore, using ID-level access here is
			// correct when available.
			RecordIterator viaIds = ((LmdbIdTripleSource) tripleSource)
					.getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, keyBuffers, reuse,
							quadReuse);
			if (viaIds != null) {
				return viaIds;
			}
		}

		// Fallback: Value-based overlay path with per-statement ID resolution (minimal unavoidable materialization).
		try {
			Value subjValue = valueForQuery(patternIds[TripleStore.SUBJ_IDX], binding, subjIndex, true, false);
			Value predValue = valueForQuery(patternIds[TripleStore.PRED_IDX], binding, predIndex, false, true);
			Value objValue = valueForQuery(patternIds[TripleStore.OBJ_IDX], binding, objIndex, false, false);

			Resource subjRes = subjValue == null ? null : (Resource) subjValue;
			IRI predIri = predValue == null ? null : (IRI) predValue;

			long ctxQueryId = selectQueryId(patternIds[TripleStore.CONTEXT_IDX], binding, ctxIndex);
			boolean requireDefaultContext = ctxQueryId == 0;
			Resource[] contexts;
			if (ctxQueryId > 0) {
				Value ctxValue = valueStore.getLazyValue(ctxQueryId);
				if (!(ctxValue instanceof Resource)) {
					return LmdbIdJoinIterator.emptyRecordIterator();
				}
				Resource ctxRes = (Resource) ctxValue;
				if (ctxRes.isTriple()) {
					return LmdbIdJoinIterator.emptyRecordIterator();
				}
				contexts = new Resource[] { ctxRes };
			} else {
				contexts = new Resource[0];
			}

			CloseableIteration<? extends Statement> stmts = contexts.length == 0
					? tripleSource.getStatements(subjRes, predIri, objValue)
					: tripleSource.getStatements(subjRes, predIri, objValue, contexts);

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
		} catch (QueryEvaluationException e) {
			throw e;
		} catch (Exception e) {
			throw new QueryEvaluationException(e);
		}
	}

	@Override
	public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order) throws QueryEvaluationException {
		return LmdbEvaluationDataset.super.getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex,
				patternIds, order, null, null);
	}

	@Override
	public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order, long[] reuse) throws QueryEvaluationException {
		return LmdbEvaluationDataset.super.getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex,
				patternIds, order, reuse, null);
	}

	@Override
	public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order, long[] reuse, long[] quadReuse)
			throws QueryEvaluationException {
		return LmdbEvaluationDataset.super.getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex,
				patternIds, order, reuse, quadReuse);
	}

	@Override
	public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order, KeyRangeBuffers keyBuffers, long[] bindingReuse,
			long[] quadReuse) throws QueryEvaluationException {
		if (order == null) {
			return getRecordIteratorInternal(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, keyBuffers,
					bindingReuse, quadReuse);
		}
		return LmdbEvaluationDataset.super.getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex,
				patternIds, order, keyBuffers, bindingReuse, quadReuse);
	}

	@Override
	public RecordIterator getOrderedRecordIterator(StatementPattern pattern, BindingSet bindings, StatementOrder order,
			KeyRangeBuffers keyBuffers) throws QueryEvaluationException {
		if (order == null) {
			return getRecordIteratorInternal(pattern, bindings, keyBuffers);
		}
		return LmdbEvaluationDataset.super.getOrderedRecordIterator(pattern, bindings, order, keyBuffers);
	}

	@Override
	public ValueStore getValueStore() {
		return valueStore;
	}

	@Override
	public String selectBestIndex(long subj, long pred, long obj, long context) {
		var currentDataset = LmdbEvaluationStrategy.getCurrentDataset();
		if (currentDataset.isPresent()) {
			LmdbEvaluationDataset delegate = currentDataset.get();
			if (delegate != this) {
				return delegate.selectBestIndex(subj, pred, obj, context);
			}
		}
		return null;
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
		long[] out = Arrays.copyOf(binding, binding.length);
		if (!applyValue(out, subjIndex, subjId)) {
			return null;
		}
		if (!applyValue(out, predIndex, predId)) {
			return null;
		}
		if (!applyValue(out, objIndex, objId)) {
			return null;
		}
		if (!applyValue(out, ctxIndex, ctxId)) {
			return null;
		}
		return out;
	}

	private boolean applyValue(long[] target, int index, long value) {
		if (index < 0) {
			return true;
		}
		long existing = target[index];
		if (existing != LmdbValue.UNKNOWN_ID && existing != value) {
			return false;
		}
		target[index] = value;
		return true;
	}

	private Value resolveValue(Var var, BindingSet bindings) {
		if (var == null) {
			return null;
		}
		if (var.hasValue()) {
			return var.getValue();
		}
		if (bindings != null) {
			return bindings.getValue(var.getName());
		}
		return null;
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
		return valueStore.getId(value, true);
	}
}
