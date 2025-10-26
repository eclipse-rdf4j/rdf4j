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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
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
	public RecordIterator getRecordIterator(StatementPattern pattern, LmdbIdVarBinding idBinding)
			throws QueryEvaluationException {

		// Translate known ID bindings to Values and delegate to the BindingSet-based variant
		org.eclipse.rdf4j.query.impl.MapBindingSet bs = new org.eclipse.rdf4j.query.impl.MapBindingSet();
		try {
			org.eclipse.rdf4j.query.algebra.Var s = pattern.getSubjectVar();
			if (s != null && !s.hasValue()) {
				long id = idBinding == null ? org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID
						: idBinding.getIdOrUnknown(s.getName());
				if (id >= 0) { // allow 0 only if context; for subject 0 won't occur
					bs.addBinding(s.getName(), valueStore.getLazyValue(id));
				}
			}
			org.eclipse.rdf4j.query.algebra.Var p = pattern.getPredicateVar();
			if (p != null && !p.hasValue()) {
				long id = idBinding == null ? org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID
						: idBinding.getIdOrUnknown(p.getName());
				if (id >= 0) {
					bs.addBinding(p.getName(), valueStore.getLazyValue(id));
				}
			}
			org.eclipse.rdf4j.query.algebra.Var o = pattern.getObjectVar();
			if (o != null && !o.hasValue()) {
				long id = idBinding == null ? org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID
						: idBinding.getIdOrUnknown(o.getName());
				if (id >= 0) {
					bs.addBinding(o.getName(), valueStore.getLazyValue(id));
				}
			}
			org.eclipse.rdf4j.query.algebra.Var c = pattern.getContextVar();
			if (c != null && !c.hasValue()) {
				long id = idBinding == null ? org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID
						: idBinding.getIdOrUnknown(c.getName());
				if (id > 0) { // context id 0 means default graph (treated as unbound in this overlay)
					bs.addBinding(c.getName(), valueStore.getLazyValue(id));
				}
			}
		} catch (Exception e) {
			throw new QueryEvaluationException(e);
		}

		return getRecordIterator(pattern, bs);
	}

	@Override
	public ValueStore getValueStore() {
		return valueStore;
	}

	@Override
	public boolean hasTransactionChanges() {
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
}
