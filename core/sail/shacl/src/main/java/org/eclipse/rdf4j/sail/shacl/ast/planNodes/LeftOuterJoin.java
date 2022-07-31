/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

/**
 * @author Håvard Ottestad
 */
public class LeftOuterJoin implements PlanNode {

	private final PlanNode left;
	private final PlanNode right;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public LeftOuterJoin(PlanNode left, PlanNode right) {
		this.left = PlanNodeHelper.handleSorting(this, left);
		this.right = PlanNodeHelper.handleSorting(this, right);

	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final CloseableIteration<? extends ValidationTuple, SailException> leftIterator = left.iterator();
			final CloseableIteration<? extends ValidationTuple, SailException> rightIterator = right.iterator();

			ValidationTuple next;
			ValidationTuple nextLeft;
			ValidationTuple nextRight;
			ValidationTuple prevLeft;

			void calculateNext() {
				if (next != null) {
					return;
				}

				if (nextLeft == null && leftIterator.hasNext()) {
					nextLeft = leftIterator.next();
				}

				if (nextLeft == null) {
					return;
				}

				if (nextRight == null && rightIterator.hasNext()) {
					nextRight = rightIterator.next();
				}

				while (next == null) {

					if (nextRight != null) {

						if (nextLeft.sameTargetAs(nextRight)) {
							next = ValidationTupleHelper.join(nextLeft, nextRight);
							prevLeft = nextLeft;
							nextRight = null;
						} else {

							int compareTo = nextLeft.compareActiveTarget(nextRight);

							if (compareTo < 0) {
								if (prevLeft != nextLeft) {
									prevLeft = nextLeft;
									next = nextLeft;
								} else if (leftIterator.hasNext()) {
									nextLeft = leftIterator.next();
								} else {
									nextLeft = null;
									break;
								}
							} else {
								if (rightIterator.hasNext()) {
									nextRight = rightIterator.next();
								} else {
									nextRight = null;
								}
							}

						}
					} else {
						if (prevLeft == nextLeft) {
							if (leftIterator.hasNext()) {
								nextLeft = leftIterator.next();
							} else {
								break;
							}
						} else {
							prevLeft = nextLeft;
							next = nextLeft;
						}

					}
				}

			}

			@Override
			public void localClose() throws SailException {
				try {
					leftIterator.close();
				} finally {
					rightIterator.close();
				}
			}

			@Override
			protected boolean localHasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			protected ValidationTuple loggingNext() throws SailException {
				calculateNext();
				ValidationTuple temp = next;
				next = null;
				return temp;
			}

		};
	}

	@Override
	public int depth() {
		return Math.max(left.depth(), right.depth());
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		left.getPlanAsGraphvizDot(stringBuilder);

		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");
		stringBuilder.append(left.getId() + " -> " + getId() + " [label=\"left\"];").append("\n");
		stringBuilder.append(right.getId() + " -> " + getId() + " [label=\"right\"];").append("\n");
		right.getPlanAsGraphvizDot(stringBuilder);

	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public String toString() {
		return "LeftOuterJoin";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		left.receiveLogger(validationExecutionLogger);
		right.receiveLogger(validationExecutionLogger);
	}

	@Override
	public boolean producesSorted() {
		return true;
	}

	@Override
	public boolean requiresSorted() {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		LeftOuterJoin that = (LeftOuterJoin) o;
		return left.equals(that.left) && right.equals(that.right);
	}

	@Override
	public int hashCode() {
		return Objects.hash(left, right);
	}
}
