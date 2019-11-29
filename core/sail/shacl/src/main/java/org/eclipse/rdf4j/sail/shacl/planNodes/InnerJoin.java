/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author Håvard Ottestad
 *         <p>
 *         This inner join algorithm assumes the left iterator is unique for tuple[0], eg. no two tuples have the same
 *         value at index 0. The right iterator is allowed to contain duplicates.
 */
public class InnerJoin implements MultiStreamPlanNode, PlanNode {

	static private final Logger logger = LoggerFactory.getLogger(InnerJoin.class);
	private boolean printed = false;

	private PlanNode left;
	private PlanNode right;
	private CloseableIteration<Tuple, SailException> iterator;
	private PushablePlanNode joined;
	private PushablePlanNode discardedLeft;
	private PushablePlanNode discardedRight;
	private ValidationExecutionLogger validationExecutionLogger;

	public InnerJoin(PlanNode left, PlanNode right) {
		this.left = left;
		this.right = right;
	}

	public List<PlanNode> parent() {
		return Arrays.asList(left, right);
	}

	public PlanNode getJoined(Class<? extends PushablePlanNode> type) {
		if (joined != null) {
			throw new IllegalStateException();
		}
		if (type == BufferedPlanNode.class) {
			joined = new BufferedPlanNode<>(this, "Joined");
		} else {
			joined = new UnBufferedPlanNode<>(this, "Joined");

		}

		return joined;
	}

	public PlanNode getDiscardedLeft(Class<? extends PushablePlanNode> type) {
		if (discardedLeft != null) {
			throw new IllegalStateException();
		}
		if (type == BufferedPlanNode.class) {
			discardedLeft = new BufferedPlanNode<>(this, "DiscardedLeft");
		} else {
			discardedLeft = new UnBufferedPlanNode<>(this, "DiscaredLeft");

		}
		return discardedLeft;
	}

	public PlanNode getDiscardedRight(Class<? extends PushablePlanNode> type) {
		if (discardedRight != null) {
			throw new IllegalStateException();
		}
		if (type == BufferedPlanNode.class) {
			discardedRight = new BufferedPlanNode<>(this, "DiscardedRight");
		} else {
			discardedRight = new UnBufferedPlanNode<>(this, "DiscardedRight");

		}
		return discardedRight;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		throw new IllegalStateException();
	}

	public CloseableIteration<Tuple, SailException> internalIterator() {

		InnerJoin that = this;
		return new CloseableIteration<Tuple, SailException>() {

			CloseableIteration<Tuple, SailException> leftIterator = left.iterator();
			CloseableIteration<Tuple, SailException> rightIterator = right.iterator();

			Tuple next;
			Tuple nextLeft;
			Tuple nextRight;
			Tuple joinedLeft;

			void calculateNext() {
				if (next != null) {
					return;
				}

				Tuple prevLeft = nextLeft;
				if (nextLeft == null && leftIterator.hasNext()) {
					nextLeft = leftIterator.next();
				}

				if (nextRight == null && rightIterator.hasNext()) {
					nextRight = rightIterator.next();
				}

				if (nextRight == null && prevLeft == null && nextLeft != null) {
					if (discardedLeft != null) {
						discardedLeft.push(nextLeft);
					}

					return;
				}

				if (nextLeft == null) {
					if (discardedRight != null) {
						while (nextRight != null) {
							discardedRight.push(nextRight);
							if (rightIterator.hasNext()) {
								nextRight = rightIterator.next();
							} else {
								nextRight = null;
							}
						}
					}
					return;
				}

				while (next == null) {
					if (nextRight != null) {

						if (nextLeft.line.get(0) == nextRight.line.get(0)
								|| nextLeft.line.get(0).equals(nextRight.line.get(0))) {
							next = TupleHelper.join(nextLeft, nextRight);
							joinedLeft = nextLeft;
							nextRight = null;
						} else {

							int compareTo = nextLeft.compareTo(nextRight);

							if (compareTo < 0) {
								if (joinedLeft != nextLeft && discardedLeft != null) {
									discardedLeft.push(nextLeft);
								}
								if (leftIterator.hasNext()) {
									nextLeft = leftIterator.next();
								} else {
									nextLeft = null;
									break;
								}
							} else {
								if (discardedRight != null) {
									discardedRight.push(nextRight);
								}
								if (rightIterator.hasNext()) {
									nextRight = rightIterator.next();
								} else {
									nextRight = null;
									break;
								}
							}

						}
					} else {
						if (discardedLeft != null) {
							while (leftIterator.hasNext()) {
								discardedLeft.push(leftIterator.next());
							}
						}

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
			public boolean hasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			public Tuple next() throws SailException {
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

		if (discardedRight != null) {
			stringBuilder.append(getId() + " -> " + (discardedRight).getId() + " [label=\"discardedRight\"];")
					.append("\n");

		}
		if (discardedLeft != null) {
			stringBuilder.append(getId() + " -> " + (discardedLeft).getId() + " [label=\"discardedLeft\"];")
					.append("\n");
		}

		if (joined != null) {
			stringBuilder.append(getId() + " -> " + (joined).getId() + " [label=\"joined\"];").append("\n");
		}
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public IteratorData getIteratorDataType() {
		if (left.getIteratorDataType() == right.getIteratorDataType()) {
			return left.getIteratorDataType();
		}

		throw new IllegalStateException("Not implemented support for when left and right have different types of data");

	}

	@Override
	public String toString() {
		return "InnerJoin";
	}

	private String leadingSpace() {
		return StringUtils.leftPad("", depth(), "    ");
	}

	@Override
	public void init() {
		if (iterator == null) {
			iterator = internalIterator();
		}
	}

	@Override
	public void close() {

		if ((discardedLeft == null || discardedLeft.isClosed()) && (discardedRight == null || discardedRight.isClosed())
				&& (joined == null || joined.isClosed())) {
			iterator.close();
			iterator = null;
		}

	}

	@Override
	public boolean incrementIterator() {

		if (iterator.hasNext()) {
			Tuple next = iterator.next();
			if (joined != null) {
				joined.push(next);
			}
			return true;
		}

		return false;
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;

		PlanNode[] planNodes = { joined, discardedLeft, discardedRight, left, right };

		for (PlanNode planNode : planNodes) {
			if (planNode != null)
				planNode.receiveLogger(validationExecutionLogger);
		}

	}
}
