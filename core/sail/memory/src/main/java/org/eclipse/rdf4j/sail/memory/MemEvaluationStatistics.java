/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.base.SketchBasedJoinEstimator;
import org.eclipse.rdf4j.sail.memory.model.MemIRI;
import org.eclipse.rdf4j.sail.memory.model.MemResource;
import org.eclipse.rdf4j.sail.memory.model.MemStatementList;
import org.eclipse.rdf4j.sail.memory.model.MemValue;
import org.eclipse.rdf4j.sail.memory.model.MemValueFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Uses the MemoryStore's statement sizes to give cost estimates based on the size of the expected results. This process
 * could be improved with repository statistics about size and distribution of statements.
 *
 * @author Arjohn Kampman
 * @author James Leigh
 */
class MemEvaluationStatistics extends EvaluationStatistics {

	private final MemValueFactory valueFactory;
	private final MemStatementList memStatementList;
	private final SketchBasedJoinEstimator sketchBasedJoinEstimator;

	MemEvaluationStatistics(MemValueFactory valueFactory, MemStatementList memStatementList,
			SketchBasedJoinEstimator sketchBasedJoinEstimator) {
		this.valueFactory = valueFactory;
		this.memStatementList = memStatementList;
		this.sketchBasedJoinEstimator = sketchBasedJoinEstimator;
	}

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return new MemCardinalityCalculator();
	}

	@Override
	public boolean supportsJoinEstimation() {
		return sketchBasedJoinEstimator.isReady();
//		return false;
	}

	static Cache<IsomorphicJoin, Double> cache = CacheBuilder.newBuilder()
			.maximumSize(10000)
			.expireAfterAccess(100, TimeUnit.MILLISECONDS)
			.build();

	/**
	 * Cache key for join estimation that ignores variable names and blank node identifiers, but preserves the
	 * structural form of the two statement patterns. Assumes left/right args are {@link StatementPattern}s.
	 */
	static final class IsomorphicJoin extends Join {
		private final Object[] signature;
		private final int hash;

		IsomorphicJoin(Join original) {
			StatementPattern left = (StatementPattern) original.getLeftArg();
			StatementPattern right = (StatementPattern) original.getRightArg();
			this.signature = computeSignature(left, right);
			this.hash = java.util.Arrays.hashCode(signature);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof IsomorphicJoin)) {
				return false;
			}
			IsomorphicJoin o = (IsomorphicJoin) other;
			return java.util.Arrays.equals(this.signature, o.signature);
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public String toString() {
			return "IsomorphicJoin(signature=" + java.util.Arrays.toString(signature) + ")";
		}

		private static Object[] computeSignature(StatementPattern left, StatementPattern right) {
			java.util.Map<Object, Token> tokens = new java.util.HashMap<>();
			java.util.List<Object> sig = new java.util.ArrayList<>(10);
			Counters counters = new Counters();

			addPatternSignature(sig, left, tokens, counters);
			addPatternSignature(sig, right, tokens, counters);

			return sig.toArray();
		}

		private static void addPatternSignature(java.util.List<Object> sig, StatementPattern sp,
				java.util.Map<Object, Token> tokens, Counters counters) {
			sig.add(sp.getScope());
			addVarSignature(sig, sp.getSubjectVar(), tokens, counters);
			addVarSignature(sig, sp.getPredicateVar(), tokens, counters);
			addVarSignature(sig, sp.getObjectVar(), tokens, counters);
			addVarSignature(sig, sp.getContextVar(), tokens, counters);
		}

		private static void addVarSignature(java.util.List<Object> sig, Var var, java.util.Map<Object, Token> tokens,
				Counters counters) {
			if (var == null) {
				sig.add(NullToken.INSTANCE);
				return;
			}

			if (var.hasValue()) {
				Value v = var.getValue();
				if (v instanceof BNode) {
					Token t = tokens.computeIfAbsent(v, k -> new Token('b', counters.nextBNodeId++));
					sig.add(t);
				} else {
					sig.add(constantKey(v));
				}
			} else {
				Token t = tokens.computeIfAbsent(var, k -> new Token('v', counters.nextVarId++));
				sig.add(t);
			}
		}

		private static String constantKey(Value v) {
			if (v instanceof IRI) {
				return "I:" + v.stringValue();
			}
			if (v instanceof Literal) {
				Literal lit = (Literal) v;
				StringBuilder sb = new StringBuilder("L:");
				sb.append(lit.getLabel());
				lit.getLanguage()
						.ifPresentOrElse(lang -> sb.append('@').append(lang),
								() -> sb.append("^^").append(lit.getDatatype().stringValue()));
				return sb.toString();
			}
			if (v instanceof Triple) {
				return "T:" + v.stringValue();
			}
			return "V:" + v.stringValue();
		}

		private static final class Counters {
			int nextVarId = 0;
			int nextBNodeId = 0;
		}

		private static final class Token {
			final char kind;
			final int id;

			Token(char kind, int id) {
				this.kind = kind;
				this.id = id;
			}

			@Override
			public boolean equals(Object other) {
				if (this == other) {
					return true;
				}
				if (!(other instanceof Token)) {
					return false;
				}
				Token o = (Token) other;
				return kind == o.kind && id == o.id;
			}

			@Override
			public int hashCode() {
				return (kind * 31) + id;
			}
		}

		private enum NullToken {
			INSTANCE
		}
	}

	protected class MemCardinalityCalculator extends CardinalityCalculator {

		@Override
		public void meet(Join node) {
			if (supportsJoinEstimation()) {

				if (node.getLeftArg() instanceof StatementPattern && node.getRightArg() instanceof StatementPattern) {
					// this is currently the only case we can estimate

					double estimatedCardinality = 0;
					try {
						estimatedCardinality = cache.get(new IsomorphicJoin(node),
								() -> sketchBasedJoinEstimator.cardinality(node));
					} catch (ExecutionException e) {
						throw new RuntimeException(e);
					}

					if (estimatedCardinality >= 0) {
						this.cardinality = estimatedCardinality;
						node.setCostEstimate(estimatedCardinality);
						return;
					}
				}
			}

			super.meet(node);
		}

		@Override
		public double getCardinality(StatementPattern sp) {

			Value subj = getConstantValue(sp.getSubjectVar());
			if (!(subj != null && subj.isResource())) {
				// can happen when a previous optimizer has inlined a comparison
				// operator.
				// this can cause, for example, the subject variable to be
				// equated to a literal value.
				// See SES-970 / SES-998
				subj = null;
			}
			Value pred = getConstantValue(sp.getPredicateVar());
			if (!(pred != null && pred.isIRI())) {
				// can happen when a previous optimizer has inlined a comparison
				// operator. See SES-970 / SES-998
				pred = null;
			}
			Value obj = getConstantValue(sp.getObjectVar());
			Value context = getConstantValue(sp.getContextVar());
			if (!(context != null && context.isResource())) {
				// can happen when a previous optimizer has inlined a comparison
				// operator. See SES-970 / SES-998
				context = null;
			}

			if (subj == null && pred == null && obj == null && context == null) {
				return memStatementList.size();
			} else {
				return minStatementCount(subj, pred, obj, context);
			}

		}

		private int minStatementCount(Value subj, Value pred, Value obj, Value context) {
			int minListSizes = Integer.MAX_VALUE;

			if (subj != null) {
				MemResource memSubj = valueFactory.getMemResource((Resource) subj);
				if (memSubj != null) {
					minListSizes = memSubj.getSubjectStatementCount();
					if (minListSizes == 0) {
						return 0;
					}
				} else {
					// couldn't find the value in the value factory, which means that there are no statements with that
					// value
					return 0;
				}
			}

			if (pred != null) {
				MemIRI memPred = valueFactory.getMemURI((IRI) pred);
				if (memPred != null) {
					minListSizes = Math.min(minListSizes, memPred.getPredicateStatementCount());
					if (minListSizes == 0) {
						return 0;
					}
				} else {
					// couldn't find the value in the value factory, which means that there are no statements with that
					// value
					return 0;
				}
			}

			if (obj != null) {
				MemValue memObj = valueFactory.getMemValue(obj);
				if (memObj != null) {
					minListSizes = Math.min(minListSizes, memObj.getObjectStatementCount());
					if (minListSizes == 0) {
						return 0;
					}
				} else {
					// couldn't find the value in the value factory, which means that there are no statements with that
					// value
					return 0;
				}
			}

			if (context != null) {
				MemResource memContext = valueFactory.getMemResource((Resource) context);
				if (memContext != null) {
					minListSizes = Math.min(minListSizes, memContext.getContextStatementCount());
				} else {
					// couldn't find the value in the value factory, which means that there are no statements with that
					// value
					return 0;
				}
			}

			assert minListSizes != Integer.MAX_VALUE : "minListSizes should have been updated before this point";

			return minListSizes;
		}

		protected Value getConstantValue(Var var) {
			if (var != null) {
				return var.getValue();
			}

			return null;
		}
	}

}
