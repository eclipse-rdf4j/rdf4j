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
package org.eclipse.rdf4j.sail.lmdb.join;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.sail.lmdb.IdAccessor;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.eclipse.rdf4j.sail.lmdb.TripleStore;
import org.eclipse.rdf4j.sail.lmdb.ValueStore;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * Join iterator that operates on LMDB internal IDs (long arrays) instead of binding sets.
 */
public class LmdbIdJoinIterator extends LookAheadIteration<BindingSet> {

	@FunctionalInterface
	interface RecordIteratorFactory {
		RecordIterator apply(long[] leftRecord) throws QueryEvaluationException;
	}

	private static final RecordIterator EMPTY_RECORD_ITERATOR = new RecordIterator() {
		@Override
		public long[] next() {
			return null;
		}

		@Override
		public void close() {
			// no-op
		}
	};

	public static RecordIterator emptyRecordIterator() {
		return EMPTY_RECORD_ITERATOR;
	}

	public static final class PatternInfo implements IdAccessor {
		private final Map<String, int[]> indexByVar;
		private final Set<String> variableNames;
		private final boolean hasContextVar;

		private PatternInfo(Map<String, int[]> indexByVar, boolean hasContextVar) {
			this.indexByVar = indexByVar;
			this.variableNames = Collections.unmodifiableSet(indexByVar.keySet());
			this.hasContextVar = hasContextVar;
		}

		static PatternInfo create(StatementPattern pattern) {
			Map<String, int[]> map = new HashMap<>();
			registerVar(map, pattern.getSubjectVar(), TripleStore.SUBJ_IDX);
			registerVar(map, pattern.getPredicateVar(), TripleStore.PRED_IDX);
			registerVar(map, pattern.getObjectVar(), TripleStore.OBJ_IDX);
			boolean hasContext = registerVar(map, pattern.getContextVar(), TripleStore.CONTEXT_IDX);
			return new PatternInfo(map, hasContext);
		}

		private static boolean registerVar(Map<String, int[]> map, Var var, int index) {
			if (var == null || var.hasValue()) {
				return false;
			}
			map.compute(var.getName(), (k, v) -> {
				if (v == null) {
					return new int[] { index };
				}
				int[] expanded = Arrays.copyOf(v, v.length + 1);
				expanded[v.length] = index;
				return expanded;
			});
			return index == TripleStore.CONTEXT_IDX;
		}

		@Override
		public Set<String> getVariableNames() {
			return variableNames;
		}

		@Override
		public int getRecordIndex(String varName) {
			int[] indices = indexByVar.get(varName);
			if (indices == null || indices.length == 0) {
				return -1;
			}
			return indices[0];
		}

		boolean hasContextVar() {
			return hasContextVar;
		}

		public int getPositionsMask(String varName) {
			int[] indices = indexByVar.get(varName);
			if (indices == null) {
				return 0;
			}
			int mask = 0;
			for (int idx : indices) {
				mask |= (1 << idx);
			}
			return mask;
		}

		@Override
		public long getId(long[] record, String varName) {
			int[] indices = indexByVar.get(varName);
			if (indices == null || indices.length == 0) {
				return LmdbValue.UNKNOWN_ID;
			}
			return record[indices[0]];
		}

		boolean applyRecord(long[] record, MutableBindingSet target, ValueStore valueStore)
				throws QueryEvaluationException {
			for (Map.Entry<String, int[]> entry : indexByVar.entrySet()) {
				String name = entry.getKey();
				int[] indices = entry.getValue();
				Value existing = target.getValue(name);

				// Fast path: if an existing binding is an LmdbValue from the same store,
				// compare IDs directly and avoid resolving candidate Values.
				if (existing instanceof LmdbValue) {
					LmdbValue lmdbExisting = (LmdbValue) existing;
					if (lmdbExisting.getValueStoreRevision().getValueStore() == valueStore) {
						long existingId = lmdbExisting.getInternalID();
						if (existingId != LmdbValue.UNKNOWN_ID) {
							for (int index : indices) {
								long id = record[index];
								// Context id of 0 is effectively null; conflicts with an existing non-null
								if (index == TripleStore.CONTEXT_IDX && id == 0L) {
									return false;
								}
								if (id != LmdbValue.UNKNOWN_ID && id != existingId) {
									return false;
								}
							}
							continue; // this variable satisfied
						}
					}
				}

				// General path: collect a consistent candidate id across positions (if any),
				// minimize value resolutions to at most one per variable.
				long chosenId = LmdbValue.UNKNOWN_ID;
				boolean haveId = false;
				for (int index : indices) {
					long id = record[index];
					// Treat default context (0) as null candidate; it conflicts with an existing non-null value.
					if (index == TripleStore.CONTEXT_IDX && id == 0L) {
						if (existing != null) {
							return false;
						}
						continue;
					}
					if (id == LmdbValue.UNKNOWN_ID) {
						continue;
					}
					if (existing != null) {
						// Fallback: existing is non-LMDB or unknown id; resolve candidate once and compare
						Value candidate = resolveValue(id, index, valueStore);
						if (candidate == null || !existing.equals(candidate)) {
							return false;
						}
					} else {
						if (!haveId) {
							chosenId = id;
							haveId = true;
						} else if (chosenId != id) {
							// Variable appears in multiple positions with conflicting ids
							return false;
						}
					}
				}

				if (existing == null && haveId) {
					Value candidate = resolveValue(chosenId, TripleStore.SUBJ_IDX /* ignored */, valueStore);
					if (candidate != null) {
						target.setBinding(name, candidate);
					}
				}
			}
			return true;
		}

		private Value resolveValue(long id, int position, ValueStore valueStore) throws QueryEvaluationException {
			if (id == LmdbValue.UNKNOWN_ID) {
				return null;
			}
			if (position == TripleStore.CONTEXT_IDX && id == 0L) {
				return null;
			}
			try {
				return valueStore.getLazyValue(id);
			} catch (IOException e) {
				throw new QueryEvaluationException(e);
			}
		}
	}

	private final RecordIterator leftIterator;
	private final RecordIteratorFactory rightFactory;
	private final PatternInfo leftInfo;
	private final PatternInfo rightInfo;
	private final Set<String> sharedVariables;
	private final QueryEvaluationContext context;
	private final BindingSet initialBindings;
	private final ValueStore valueStore;

	private RecordIterator currentRightIterator;
	private long[] currentLeftRecord;
	private BindingSet currentLeftBinding;

	LmdbIdJoinIterator(RecordIterator leftIterator, RecordIteratorFactory rightFactory, PatternInfo leftInfo,
			PatternInfo rightInfo, Set<String> sharedVariables, QueryEvaluationContext context,
			BindingSet initialBindings, ValueStore valueStore) {
		this.leftIterator = leftIterator;
		this.rightFactory = rightFactory;
		this.leftInfo = leftInfo;
		this.rightInfo = rightInfo;
		this.sharedVariables = sharedVariables;
		this.context = context;
		this.initialBindings = initialBindings;
		this.valueStore = valueStore;
	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		while (true) {
			if (currentRightIterator != null) {
				long[] rightRecord;
				while ((rightRecord = nextRecord(currentRightIterator)) != null) {
					if (!matchesJoin(currentLeftRecord, rightRecord)) {
						continue;
					}
					MutableBindingSet result = context.createBindingSet(initialBindings);
					if (!leftInfo.applyRecord(currentLeftRecord, result, valueStore)) {
						continue;
					}
					if (!rightInfo.applyRecord(rightRecord, result, valueStore)) {
						continue;
					}
					return result;
				}
				currentRightIterator.close();
				currentRightIterator = null;
			}

			long[] leftRecord = nextRecord(leftIterator);
			if (leftRecord == null) {
				return null;
			}

			currentLeftRecord = leftRecord;
			currentLeftBinding = null;

			currentRightIterator = rightFactory.apply(leftRecord);
			if (currentRightIterator == null) {
				currentRightIterator = emptyRecordIterator();
			}
		}
	}

	private boolean matchesJoin(long[] leftRecord, long[] rightRecord) {
		for (String name : sharedVariables) {
			long leftId = leftInfo.getId(leftRecord, name);
			long rightId = rightInfo.getId(rightRecord, name);
			if (leftId != LmdbValue.UNKNOWN_ID && rightId != LmdbValue.UNKNOWN_ID && leftId != rightId) {
				return false;
			}
		}
		return true;
	}

	private long[] nextRecord(RecordIterator iterator) throws QueryEvaluationException {
		// Avoid per-record array copy: LmdbRecordIterator returns a stable array instance
		// that is only mutated on subsequent next() calls. We consume the array fully
		// before advancing the iterator again, so returning it directly is safe here.
		return iterator.next();
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		leftIterator.close();
		if (currentRightIterator != null) {
			currentRightIterator.close();
			currentRightIterator = null;
		}
	}
}
