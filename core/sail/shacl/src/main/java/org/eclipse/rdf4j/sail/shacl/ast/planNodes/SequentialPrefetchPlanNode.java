/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Merges the results of multiple PlanNodes (originating from a split SPARQL UNION) into a single stream, in the
 * consumer's own thread - sequentially, delegate by delegate.
 * <p>
 * De-duplicates across delegates by ValidationTuple equality, restoring the behavior that the old textual-UNION query
 * got "for free" from a single outer SELECT DISTINCT (which collapsed duplicate rows coming from different UNION
 * branches). Without this, the same (target[,value]) pair satisfied by two different delegates would otherwise be
 * emitted twice.
 *
 * @author Sava Savov
 */
public class SequentialPrefetchPlanNode implements PlanNode {

	private final List<PlanNode> delegates;
	private ValidationExecutionLogger validationExecutionLogger;

	public SequentialPrefetchPlanNode(int delegatesSize) {
		this.delegates = new ArrayList<>(delegatesSize);
	}

	public void addDelegate(PlanNode planNode) {
		delegates.add(planNode);
	}

	@Override
	public CloseableIteration<? extends ValidationTuple> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			private final Set<ValidationTuple> seen = new HashSet<>();
			private int delegateIndex = 0;
			private CloseableIteration<? extends ValidationTuple> currentIterator;
			private ValidationTuple next;
			private boolean done = false;

			@Override
			protected void init() {
				// Intentionally no-op: iterators are opened lazily, per-delegate, in advance() below.
				// This runs after receiveLogger(...) has already been propagated to this node and its
				// delegates by the surrounding plan-setup code, which is what we need - see the
				// class-level comment on the sequencing problem this avoids.
			}

			private boolean openNextDelegateIfNeeded() {
				while (currentIterator == null && delegateIndex < delegates.size()) {
					currentIterator = delegates.get(delegateIndex).iterator();
					delegateIndex++;
					if (currentIterator.hasNext()) {
						return true;
					}
					// exhausted immediately (e.g. EmptyNode); close and move to the next delegate
					currentIterator.close();
					currentIterator = null;
				}
				return currentIterator != null;
			}

			private void advance() {
				if (next != null || done) {
					return;
				}
				try {
					while (next == null) {
						if (!openNextDelegateIfNeeded()) {
							done = true;
							return;
						}
						if (!currentIterator.hasNext()) {
							currentIterator.close();
							currentIterator = null;
							continue;
						}
						ValidationTuple candidate = currentIterator.next();
						if (seen.add(candidate)) {
							next = candidate;
						}
						// else: duplicate seen from an earlier delegate - loop continues
					}
				} catch (Throwable t) {
					done = true;
					if (currentIterator != null) {
						currentIterator.close();
						currentIterator = null;
					}
					throw (t instanceof SailException) ? (SailException) t : new SailException(t);
				}
			}

			@Override
			protected boolean localHasNext() {
				advance();
				return next != null;
			}

			@Override
			protected ValidationTuple loggingNext() {
				advance();
				ValidationTuple t = next;
				next = null;
				return t;
			}

			@Override
			public void localClose() {
				if (currentIterator != null) {
					currentIterator.close();
				}
			}
		};
	}

	@Override
	public int depth() {
		return delegates.stream().mapToInt(PlanNode::depth).max().orElse(0) + 1;
	}

	@Override
	public boolean producesSorted() {
		return false;
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		delegates.forEach(d -> d.receiveLogger(validationExecutionLogger));
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		for (PlanNode delegate : delegates) {
			delegate.getPlanAsGraphvizDot(stringBuilder);
		}
	}

	@Override
	public String getId() {
		return "_:" + System.identityHashCode(this);
	}

	@Override
	public String toString() {
		return "SequentialPrefetchPlanNode{" + delegates + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SequentialPrefetchPlanNode that = (SequentialPrefetchPlanNode) o;
		return delegates.equals(that.delegates);
	}

	@Override
	public int hashCode() {
		return Objects.hash(delegates);
	}
}
