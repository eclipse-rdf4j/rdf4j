/***/
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

/**
 * An {@link LmdbEvaluationDataset} implementation that reads via the delegated Sail layer (through the
 * {@link TripleSource}) to ensure transaction overlays and isolation levels are honored, while exposing LMDB record
 * iterators for the ID join.
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
		if (ctxVal instanceof Resource && ((Resource) ctxVal).isTriple()) {
			return LmdbIdJoinIterator.emptyRecordIterator();
		}

		Resource[] contexts;
		if (ctxVal == null) {
			contexts = new Resource[0]; // no restriction: all contexts
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
					// ensure iterator is closed on error
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
		try {
			Value subjValue = valueForQuery(patternIds[TripleStore.SUBJ_IDX], binding, subjIndex, true, false);
			Resource subjRes = subjValue == null ? null : (Resource) subjValue;

			Value predValue = valueForQuery(patternIds[TripleStore.PRED_IDX], binding, predIndex, false, true);
			IRI predIri = predValue == null ? null : (IRI) predValue;

			Value objValue = valueForQuery(patternIds[TripleStore.OBJ_IDX], binding, objIndex, false, false);

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
				requireDefaultContext = false;
			} else {
				contexts = new Resource[0];
			}

			CloseableIteration<? extends Statement> stmts = contexts.length == 0
					? tripleSource.getStatements(subjRes, predIri, objValue)
					: tripleSource.getStatements(subjRes, predIri, objValue, contexts);

			final boolean defaultOnly = requireDefaultContext;

			return new RecordIterator() {
				private final long[] scratch = java.util.Arrays.copyOf(binding, binding.length);

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
				patternIds, order);
	}

	@Override
	public ValueStore getValueStore() {
		return valueStore;
	}

	private long selectQueryId(long patternId, long[] binding, int index) {
		if (patternId != org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID) {
			return patternId;
		}
		if (index >= 0 && index < binding.length) {
			long bound = binding[index];
			if (bound != org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID) {
				return bound;
			}
		}
		return org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID;
	}

	private Value valueForQuery(long patternId, long[] binding, int index, boolean requireResource, boolean requireIri)
			throws QueryEvaluationException {
		long id = selectQueryId(patternId, binding, index);
		if (id == org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID) {
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
			if (value instanceof Resource && ((Resource) value).isTriple()) {
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
		if (existing != org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID && existing != value) {
			return false;
		}
		target[index] = value;
		return true;
	}

	@Override
	public boolean hasTransactionChanges() {
		return false;
	}

	private Value resolveValue(Var var, BindingSet bindings) {
		if (var == null) {
			return null;
		}
		if (var.hasValue()) {
			return var.getValue();
		}
		if (bindings != null) {
			Value bound = bindings.getValue(var.getName());
			if (bound != null) {
				return bound;
			}
		}
		return null;
	}

	private long resolveId(Value value) throws Exception {
		if (value == null) {
			return org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID;
		}
		if (value instanceof org.eclipse.rdf4j.sail.lmdb.model.LmdbValue) {
			org.eclipse.rdf4j.sail.lmdb.model.LmdbValue lmdbValue = (org.eclipse.rdf4j.sail.lmdb.model.LmdbValue) value;
			if (valueStore.getRevision().equals(lmdbValue.getValueStoreRevision())) {
				return lmdbValue.getInternalID();
			}
		}
		return valueStore.getId(value);
	}

	private StatementPattern toStatementPattern(long[] patternIds, String[] varNames) throws QueryEvaluationException {
		if (patternIds == null || varNames == null || patternIds.length != 4 || varNames.length != 4) {
			throw new IllegalArgumentException("Pattern arrays must have length 4");
		}
		org.eclipse.rdf4j.query.algebra.Var subj = buildVar(patternIds[0], varNames[0], "s", true, false);
		org.eclipse.rdf4j.query.algebra.Var pred = buildVar(patternIds[1], varNames[1], "p", false, true);
		org.eclipse.rdf4j.query.algebra.Var obj = buildVar(patternIds[2], varNames[2], "o", false, false);
		org.eclipse.rdf4j.query.algebra.Var ctx = buildContextVar(patternIds[3], varNames[3]);
		return new StatementPattern(subj, pred, obj, ctx);
	}

	private org.eclipse.rdf4j.query.algebra.Var buildVar(long id, String name, String placeholder,
			boolean requireResource,
			boolean requireIri) throws QueryEvaluationException {
		if (name != null) {
			return new org.eclipse.rdf4j.query.algebra.Var(name);
		}
		if (id == org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID) {
			// Should not occur for subject/predicate/object when derived from StatementPattern
			return new org.eclipse.rdf4j.query.algebra.Var(placeholder);
		}
		try {
			org.eclipse.rdf4j.model.Value value = valueStore.getLazyValue(id);
			if (value == null) {
				throw new QueryEvaluationException("Unable to resolve value for ID " + id);
			}
			if (requireResource && !(value instanceof org.eclipse.rdf4j.model.Resource)) {
				throw new QueryEvaluationException("Expected resource value for subject ID " + id);
			}
			if (requireIri && !(value instanceof org.eclipse.rdf4j.model.IRI)) {
				throw new QueryEvaluationException("Expected IRI value for predicate ID " + id);
			}
			return new org.eclipse.rdf4j.query.algebra.Var(placeholder, value);
		} catch (Exception e) {
			if (e instanceof QueryEvaluationException) {
				throw (QueryEvaluationException) e;
			}
			throw new QueryEvaluationException(e);
		}
	}

	private org.eclipse.rdf4j.query.algebra.Var buildContextVar(long id, String name) throws QueryEvaluationException {
		if (name != null) {
			return new org.eclipse.rdf4j.query.algebra.Var(name);
		}
		if (id == org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID) {
			return null;
		}
		try {
			org.eclipse.rdf4j.model.Value value = valueStore.getLazyValue(id);
			if (value == null) {
				throw new QueryEvaluationException("Unable to resolve context value for ID " + id);
			}
			if (!(value instanceof org.eclipse.rdf4j.model.Resource)) {
				throw new QueryEvaluationException("Context ID " + id + " does not map to a resource");
			}
			org.eclipse.rdf4j.model.Resource ctx = (org.eclipse.rdf4j.model.Resource) value;
			if (ctx.isTriple()) {
				throw new QueryEvaluationException("Triple-valued contexts are not supported");
			}
			return new org.eclipse.rdf4j.query.algebra.Var("ctx", ctx);
		} catch (Exception e) {
			if (e instanceof QueryEvaluationException) {
				throw (QueryEvaluationException) e;
			}
			throw new QueryEvaluationException(e);
		}
	}
}
