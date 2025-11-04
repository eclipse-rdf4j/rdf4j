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
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailDatasetTripleSource;
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdJoinIterator;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * LMDB-aware {@link TripleSource} that exposes ID-level access when supported by the backing dataset.
 */
public class LmdbSailDatasetTripleSource extends SailDatasetTripleSource implements LmdbIdTripleSource {

	private final SailDataset dataset;

	public LmdbSailDatasetTripleSource(ValueFactory vf, SailDataset dataset) {
		super(vf, dataset);
		this.dataset = dataset;
	}

	@Override
	public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds) throws QueryEvaluationException {
		return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, null);
	}

	@Override
	public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
			long[] patternIds, long[] reuse) throws QueryEvaluationException {

		// Fast path: backing dataset supports ID-level access
		if (dataset instanceof LmdbEvaluationDataset) {
			return ((LmdbEvaluationDataset) dataset).getRecordIterator(binding, subjIndex, predIndex, objIndex,
					ctxIndex, patternIds, reuse);
		}

		// Fallback path: value-level iteration converted to IDs using ValueStore
		ValueStore valueStore = LmdbEvaluationStrategy.getCurrentDataset()
				.map(LmdbEvaluationDataset::getValueStore)
				.orElse(null);
		if (valueStore == null) {
			// No way to resolve IDs safely; return empty to avoid incorrect results
			return LmdbIdJoinIterator.emptyRecordIterator();
		}

		// Resolve fixed values for the pattern using the binding and pattern IDs
		Value subjValue = valueForQuery(valueStore, patternIds[TripleStore.SUBJ_IDX], binding, subjIndex, true, false);
		Resource subjRes = subjValue == null ? null : (Resource) subjValue;

		Value predValue = valueForQuery(valueStore, patternIds[TripleStore.PRED_IDX], binding, predIndex, false, true);
		IRI predIri = predValue == null ? null : (IRI) predValue;

		Value objValue = valueForQuery(valueStore, patternIds[TripleStore.OBJ_IDX], binding, objIndex, false, false);

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

		try {
			final CloseableIteration<? extends Statement> stmts = contexts.length == 0
					? dataset.getStatements(subjRes, predIri, objValue)
					: dataset.getStatements(subjRes, predIri, objValue, contexts);

			if (stmts instanceof EmptyIteration) {
				return LmdbIdJoinIterator.emptyRecordIterator();
			}

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
							if (requireDefaultContext && st.getContext() != null) {
								continue;
							}

							long subjId = resolveId(valueStore, st.getSubject());
							long predId = resolveId(valueStore, st.getPredicate());
							long objId = resolveId(valueStore, st.getObject());
							long ctxId = st.getContext() == null ? 0L : resolveId(valueStore, st.getContext());

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
		} catch (SailException e) {
			throw new QueryEvaluationException(e);
		}
	}

	@Override
	public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order) throws QueryEvaluationException {
		return getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, order, null);
	}

	@Override
	public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
			int ctxIndex, long[] patternIds, StatementOrder order, long[] reuse) throws QueryEvaluationException {
		if (dataset instanceof LmdbEvaluationDataset) {
			return ((LmdbEvaluationDataset) dataset).getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex,
					ctxIndex, patternIds, order, reuse);
		}
		return LmdbIdTripleSource.super.getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex,
				patternIds, order, reuse);
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

	private Value valueForQuery(ValueStore valueStore, long patternId, long[] binding, int index,
			boolean requireResource,
			boolean requireIri) throws QueryEvaluationException {
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

	private long resolveId(ValueStore valueStore, Value value) throws Exception {
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
