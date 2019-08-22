/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

public class EqualsJoin implements PlanNode {
	private PlanNode left;
	private PlanNode right;
	private boolean useAsFilter;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public EqualsJoin(PlanNode left, PlanNode right, boolean useAsFilter) {
		this.left = left;
		this.right = right;
		this.useAsFilter = useAsFilter;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			CloseableIteration<Tuple, SailException> leftIterator = left.iterator();
			CloseableIteration<Tuple, SailException> rightIterator = right.iterator();

			Tuple next;
			Tuple nextLeft;
			Tuple nextRight;

			void calculateNext() {
				if (next != null) {
					return;
				}

				if (nextLeft == null && leftIterator.hasNext()) {
					nextLeft = leftIterator.next();
				}

				if (nextRight == null && rightIterator.hasNext()) {
					nextRight = rightIterator.next();
				}

				if (nextLeft == null) {
					return;
				}

				while (next == null) {
					if (nextRight != null) {

						if (nextLeft.line == nextRight.line || nextLeft.line.equals(nextRight.line)) {
							if (useAsFilter) {
								next = nextLeft;
								next.addAllCausedByPropertyShape(nextRight.getCausedByPropertyShapes());
								next.addHistory(nextRight);
							} else {
								next = TupleHelper.join(nextLeft, nextRight);
							}
							nextRight = null;
						} else {

							int compareTo = nextLeft.compareTo(nextRight);

							if (compareTo < 0) {
								if (leftIterator.hasNext()) {
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
									break;
								}
							}

						}
					} else {
						return;
					}
				}

			}

			@Override
			public void close() throws SailException {
				leftIterator.close();
				rightIterator.close();
			}

			@Override
			boolean localHasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			Tuple loggingNext() throws SailException {
				calculateNext();
				Tuple temp = next;
				next = null;
				return temp;
			}

			@Override
			public void remove() throws SailException {

			}

		};
	}

	@Override
	public int depth() {
		return 0;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed)
			return;
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
		return null;
	}

	@Override
	public IteratorData getIteratorDataType() {
		if (left.getIteratorDataType() == right.getIteratorDataType())
			return left.getIteratorDataType();

		throw new IllegalStateException("Not implemented support for when left and right have different types of data");

	}

	@Override
	public String toString() {
		return "EqualsJoin{" + "useAsFilter=" + useAsFilter + '}';
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		left.receiveLogger(validationExecutionLogger);
		right.receiveLogger(validationExecutionLogger);
	}
}
