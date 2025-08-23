// File: core/queryalgebra-evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/ConditionalLeftJoinQueryEvaluationStep.java
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.Set;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.VarNameCollector;

/**
 * A LeftJoin evaluator that, when safe, short-circuits optional RHS evaluation: If the LeftJoin condition mentions only
 * LHS vars and EBV(condition) is false for a given LHS binding, the RHS is never evaluated.
 *
 * See also: LeftJoinQueryEvaluationStep.supply (fallback).
 */
public final class ConditionalLeftJoinQueryEvaluationStep implements QueryEvaluationStep {

	private final QueryEvaluationStep left;
	private final QueryEvaluationStep right;
	private final QueryValueEvaluationStep condition; // may be null
	private final Set<String> optionalVars; // RHS vars (for reference)
	private final EvaluationStrategy strategy;
	private final QueryEvaluationContext context;

	private ConditionalLeftJoinQueryEvaluationStep(
			EvaluationStrategy strategy,
			QueryEvaluationStep left,
			QueryEvaluationStep right,
			QueryValueEvaluationStep condition,
			Set<String> optionalVars,
			QueryEvaluationContext context) {
		this.strategy = strategy;
		this.left = left;
		this.right = right;
		this.condition = condition;
		this.optionalVars = optionalVars;
		this.context = context;
	}

	/**
	 * Try to create a conditional step. If unsafe/non-beneficial, return null.
	 */
	public static QueryEvaluationStep supplyIfBeneficial(EvaluationStrategy strategy, LeftJoin lj,
			QueryEvaluationContext context) {
		// If there is no condition at all, nothing to short-circuit.
		ValueExpr cond = lj.getCondition();
		if (cond == null) {
			return null;
		}

		// Vars used by left / condition
		Set<String> leftVars = VarNameCollector.process(lj.getLeftArg());
		Set<String> condVars = VarNameCollector.process(cond);

		// Only safe if condition uses a subset of LHS vars.
		if (!leftVars.containsAll(condVars)) {
			return null; // fallback to default
		}

		// Precompile steps
		QueryEvaluationStep left = strategy.precompile(lj.getLeftArg(), context);
		QueryEvaluationStep right = strategy.precompile(lj.getRightArg(), context);
		QueryValueEvaluationStep condStep = strategy.precompile(cond, context);

		Set<String> rhsVars = VarNameCollector.process(lj.getRightArg());
		return new ConditionalLeftJoinQueryEvaluationStep(strategy, left, right, condStep, rhsVars, context);
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet parentBindings) {
		// Evaluate left first (possibly delayed)
		CloseableIteration<BindingSet> leftIter = left.evaluate(parentBindings);

		return new AbstractCloseableIteration<BindingSet>() {
			private BindingSet currentLeft = null;
			private CloseableIteration<BindingSet> currentRight = null;
			private boolean emittedLeftForCurrent = false;

			@Override
			public boolean hasNext() {
				BindingSet next = computeNext();
				if (next != null) {
					// stash in a tiny one-item buffer by handing it to next()
					buffered = next;
					return true;
				}
				return false;
			}

			private BindingSet buffered = null;

			@Override
			public BindingSet next() {
				if (buffered != null) {
					BindingSet tmp = buffered;
					buffered = null;
					return tmp;
				}
				BindingSet n = computeNext();
				if (n == null) {
					throw new java.util.NoSuchElementException();
				}
				return n;
			}

			private BindingSet computeNext() {
				try {
					while (true) {
						// If we have an active RHS iterator, drain it
						if (currentRight != null) {
							if (currentRight.hasNext()) {
								BindingSet r = currentRight.next();
								return merge(currentLeft, r);
							} else {
								currentRight.close();
								currentRight = null;
								if (!emittedLeftForCurrent) {
									emittedLeftForCurrent = true;
									return currentLeft; // OPTIONAL case: no RHS rows; emit plain left
								}
								// else continue to fetch a new left
							}
						}

						// Fetch next left row
						if (!leftIter.hasNext()) {
							return null;
						}
						currentLeft = leftIter.next();
						emittedLeftForCurrent = false;

						// EBV(short-circuit) on the LHS
						boolean pass = true;
						if (condition != null) {
							// Evaluate condition for this left binding (no RHS vars present by construction)
							pass = QueryEvaluationUtil.getEffectiveBooleanValue(condition.evaluate(currentLeft));
						}

						if (!pass) {
							// condition false ⇒ OPTIONAL cannot match: emit left immediately; skip RHS entirely.
							emittedLeftForCurrent = true;
							return currentLeft;
						}

						// condition true ⇒ evaluate RHS with injected left bindings
						currentRight = right.evaluate(currentLeft);
						// loop continues: will drain RHS or emit left if empty
					}
				} catch (Exception e) {
					// normalize to unchecked to keep interface clean
					throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
				}
			}

			@Override
			protected void handleClose() {
				try {
					if (currentRight != null) {
						currentRight.close();
					}
				} finally {
					if (leftIter != null) {
						leftIter.close();
					}
				}
			}

			// Merge without overwriting existing LHS bindings (standard OPTIONAL semantics).
			private BindingSet merge(BindingSet left, BindingSet right) {
				// QueryBindingSet keeps insertion order and avoids re-alloc churn
				org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet out = new org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet(
						left.size() + right.size());
				out.addAll(left);
				right.forEach(b -> {
					if (!out.hasBinding(b.getName())) {
						out.addBinding(b);
					}
				});
				return out;
			}
		};
	}
}
