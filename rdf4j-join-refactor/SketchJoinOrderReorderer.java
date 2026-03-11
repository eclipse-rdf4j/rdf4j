package org.eclipse.rdf4j.sail.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.JoinOrderPlanner;

/**
 * Dedicated join-order search for {@link SketchBasedJoinEstimator}.
 *
 * <p>
 * This class implements a standard left-deep dynamic-programming enumerator for small join sets and a connected-first
 * greedy fallback for larger join sets. The search logic is intentionally isolated here so that all join reordering is
 * in one place.
 * </p>
 */
final class SketchJoinOrderReorderer {

	private static final int MAX_DYNAMIC_PROGRAMMING_JOIN_ARGS = 20;

	private final SketchBasedJoinEstimator estimator;

	SketchJoinOrderReorderer(SketchBasedJoinEstimator estimator) {
		this.estimator = Objects.requireNonNull(estimator, "estimator");
	}

	Optional<JoinOrderPlanner.JoinOrderPlan> plan(List<TupleExpr> args, Set<String> initiallyBoundVars,
			JoinOrderPlanner.Algorithm algorithm) {
		if (args == null || args.isEmpty()) {
			return Optional.empty();
		}

		List<TupleExpr> expressions = List.copyOf(args);
		Set<String> bound = initiallyBoundVars == null ? Collections.emptySet() : Set.copyOf(initiallyBoundVars);
		List<Term> terms = new ArrayList<>(expressions.size());
		for (int index = 0; index < expressions.size(); index++) {
			SketchBasedJoinEstimator.TuplePlanEstimate estimate = estimator.planEstimateForJoinOrdering(
					expressions.get(index), bound);
			if (estimate == null) {
				return Optional.empty();
			}
			terms.add(new Term(index, estimate));
		}

		Plan bestPlan = algorithm == JoinOrderPlanner.Algorithm.DYNAMIC_PROGRAMMING
				&& terms.size() <= MAX_DYNAMIC_PROGRAMMING_JOIN_ARGS
						? dynamicProgramming(terms)
						: greedy(terms);

		if (bestPlan == null) {
			return Optional.empty();
		}

		List<TupleExpr> orderedArgs = new ArrayList<>(bestPlan.order.size());
		for (Integer index : bestPlan.order) {
			orderedArgs.add(expressions.get(index));
		}

		return Optional.of(new JoinOrderPlanner.JoinOrderPlan(orderedArgs, bestPlan.rows, bestPlan.totalWork));
	}

	private Plan dynamicProgramming(List<Term> terms) {
		int size = terms.size();
		if (size == 0) {
			return null;
		}
		if (size == 1) {
			return seed(terms.get(0));
		}

		int stateCount = 1 << size;
		Plan[] bestByMask = new Plan[stateCount];
		for (Term term : terms) {
			bestByMask[1 << term.index] = seed(term);
		}

		for (int mask = 1; mask < stateCount; mask++) {
			if (Integer.bitCount(mask) < 2) {
				continue;
			}

			Plan best = null;
			for (Term term : terms) {
				int bit = 1 << term.index;
				if ((mask & bit) == 0) {
					continue;
				}

				Plan prefix = bestByMask[mask ^ bit];
				if (prefix == null) {
					continue;
				}

				Plan candidate = append(prefix, term);
				if (isBetter(candidate, best)) {
					best = candidate;
				}
			}
			bestByMask[mask] = best;
		}

		return bestByMask[stateCount - 1];
	}

	private Plan greedy(List<Term> terms) {
		if (terms.isEmpty()) {
			return null;
		}

		Plan current = null;
		for (Term term : terms) {
			Plan candidate = seed(term);
			if (isBetter(candidate, current)) {
				current = candidate;
			}
		}

		if (current == null) {
			return null;
		}

		boolean[] used = new boolean[terms.size()];
		for (Integer index : current.order) {
			used[index] = true;
		}

		while (current.order.size() < terms.size()) {
			Plan bestConnected = null;
			Plan bestDisconnected = null;
			Term bestConnectedTerm = null;
			Term bestDisconnectedTerm = null;

			for (Term term : terms) {
				if (used[term.index]) {
					continue;
				}

				Plan candidate = append(current, term);
				if (estimator.hasSharedJoinVariable(current.estimate, term.estimate)) {
					if (isBetter(candidate, bestConnected)) {
						bestConnected = candidate;
						bestConnectedTerm = term;
					}
				} else if (isBetter(candidate, bestDisconnected)) {
					bestDisconnected = candidate;
					bestDisconnectedTerm = term;
				}
			}

			Term nextTerm = bestConnectedTerm != null ? bestConnectedTerm : bestDisconnectedTerm;
			Plan nextPlan = bestConnectedTerm != null ? bestConnected : bestDisconnected;
			if (nextTerm == null || nextPlan == null) {
				return current;
			}

			used[nextTerm.index] = true;
			current = nextPlan;
		}

		return current;
	}

	private Plan seed(Term term) {
		double rows = term.estimate.outputRows();
		return new Plan(List.of(term.index), rows, rows, 0, term.estimate);
	}

	private Plan append(Plan prefix, Term next) {
		SketchBasedJoinEstimator.JoinStepEstimate step = estimator.estimateJoinStepForJoinOrdering(prefix.estimate,
				next.estimate);
		boolean connected = estimator.hasSharedJoinVariable(prefix.estimate, next.estimate);

		List<Integer> order = new ArrayList<>(prefix.order.size() + 1);
		order.addAll(prefix.order);
		order.add(next.index);

		return new Plan(order, step.outputRows(), prefix.totalWork + step.workRows(),
				prefix.crossJoins + (connected ? 0 : 1), estimator.joinedPlanEstimate(step));
	}

	private boolean isBetter(Plan candidate, Plan incumbent) {
		if (candidate == null) {
			return false;
		}
		if (incumbent == null) {
			return true;
		}

		int workComparison = Double.compare(candidate.totalWork, incumbent.totalWork);
		if (workComparison != 0) {
			return workComparison < 0;
		}

		int rowsComparison = Double.compare(candidate.rows, incumbent.rows);
		if (rowsComparison != 0) {
			return rowsComparison < 0;
		}

		int crossJoinComparison = Integer.compare(candidate.crossJoins, incumbent.crossJoins);
		if (crossJoinComparison != 0) {
			return crossJoinComparison < 0;
		}

		return compareOrder(candidate.order, incumbent.order) < 0;
	}

	private int compareOrder(List<Integer> left, List<Integer> right) {
		int size = Math.min(left.size(), right.size());
		for (int i = 0; i < size; i++) {
			int comparison = Integer.compare(left.get(i), right.get(i));
			if (comparison != 0) {
				return comparison;
			}
		}
		return Integer.compare(left.size(), right.size());
	}

	private static final class Term {
		private final int index;
		private final SketchBasedJoinEstimator.TuplePlanEstimate estimate;

		private Term(int index, SketchBasedJoinEstimator.TuplePlanEstimate estimate) {
			this.index = index;
			this.estimate = estimate;
		}
	}

	private static final class Plan {
		private final List<Integer> order;
		private final double rows;
		private final double totalWork;
		private final int crossJoins;
		private final SketchBasedJoinEstimator.TuplePlanEstimate estimate;

		private Plan(List<Integer> order, double rows, double totalWork, int crossJoins,
				SketchBasedJoinEstimator.TuplePlanEstimate estimate) {
			this.order = Collections.unmodifiableList(new ArrayList<>(order));
			this.rows = rows;
			this.totalWork = totalWork;
			this.crossJoins = crossJoins;
			this.estimate = estimate;
		}
	}
}
