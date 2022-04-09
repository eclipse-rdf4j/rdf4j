/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 *         <p>
 *         This inner join algorithm assumes the left iterator is unique for tuple[0], eg. no two tuples have the same
 *         value at index 0. The right iterator is allowed to contain duplicates.
 */
public class InnerJoin implements MultiStreamPlanNode, PlanNode {

	static private final Logger logger = LoggerFactory.getLogger(InnerJoin.class);
	private StackTraceElement[] stackTrace;
	private boolean printed = false;

	private final PlanNode left;
	private final PlanNode right;
	private CloseableIteration<ValidationTuple, SailException> iterator;
	private NotifyingPushablePlanNode joined;
	private NotifyingPushablePlanNode discardedLeft;
	private NotifyingPushablePlanNode discardedRight;

	public InnerJoin(PlanNode left, PlanNode right) {
		this.left = PlanNodeHelper.handleSorting(this, left);
		this.right = PlanNodeHelper.handleSorting(this, right);

		// this.stackTrace = Thread.currentThread().getStackTrace();
	}

	public List<PlanNode> parent() {
		return Arrays.asList(left, right);
	}

	public PlanNode getJoined(Class<? extends PushablePlanNode> type) {
		if (joined != null) {
			throw new IllegalStateException();
		}
		if (type == BufferedPlanNode.class) {
			joined = new NotifyingPushablePlanNode(new BufferedPlanNode<>(this, "Joined"));
		} else {
			joined = new NotifyingPushablePlanNode(new UnBufferedPlanNode<>(this, "Joined"));

		}

		return joined;
	}

	public PlanNode getDiscardedLeft(Class<? extends PushablePlanNode> type) {
		if (discardedLeft != null) {
			throw new IllegalStateException();
		}
		if (type == BufferedPlanNode.class) {
			discardedLeft = new NotifyingPushablePlanNode(new BufferedPlanNode<>(this, "DiscardedLeft"));
		} else {
			throw new UnsupportedOperationException("All discarded nodes need to use buffered nodes");
		}
		return discardedLeft;
	}

	public PlanNode getDiscardedRight(Class<? extends PushablePlanNode> type) {
		if (discardedRight != null) {
			throw new IllegalStateException();
		}
		if (type == BufferedPlanNode.class) {
			discardedRight = new NotifyingPushablePlanNode(new BufferedPlanNode<>(this, "DiscardedRight"));
		} else {
			throw new UnsupportedOperationException("All discarded nodes need to use buffered nodes");
		}
		return discardedRight;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		throw new IllegalStateException();
	}

	public CloseableIteration<ValidationTuple, SailException> internalIterator() {

		return new CloseableIteration<ValidationTuple, SailException>() {

			final CloseableIteration<? extends ValidationTuple, SailException> leftIterator = left.iterator();
			final CloseableIteration<? extends ValidationTuple, SailException> rightIterator = right.iterator();

			ValidationTuple next;
			ValidationTuple nextLeft;
			ValidationTuple nextRight;
			ValidationTuple joinedLeft;

			void calculateNext() {
				if (next != null) {
					return;
				}

				ValidationTuple prevLeft = nextLeft;
				if (nextLeft == null && leftIterator.hasNext()) {
					nextLeft = leftIterator.next();
				}

				if (nextRight == null && rightIterator.hasNext()) {
					nextRight = rightIterator.next();
				}

				if (nextRight == null && prevLeft == null && nextLeft != null) {
					if (discardedLeft != null) {
						discardedLeft.push(nextLeft);
						while (leftIterator.hasNext()) {
							discardedLeft.push(leftIterator.next());
						}
						assert !leftIterator.hasNext() : leftIterator.toString();
					}
					nextLeft = null;

					return;
				}

				if (nextLeft == null && nextRight != null) {
					if (discardedRight != null) {
						discardedRight.push(nextRight);
						while (rightIterator.hasNext()) {
							discardedRight.push(rightIterator.next());
						}
						assert !rightIterator.hasNext() : rightIterator.toString();

					}
					nextRight = null;

					return;
				}

				while (next == null) {
					if (nextRight != null) {

						if (nextLeft.sameTargetAs(nextRight)) {
							next = ValidationTupleHelper.join(nextLeft, nextRight);
							joinedLeft = nextLeft;
							nextRight = null;
						} else {

							int compareTo = nextLeft.compareActiveTarget(nextRight);

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

							assert nextLeft == null;
						}

						assert !rightIterator.hasNext() : rightIterator.toString();

						return;
					}
				}

				if (next == null) {
					if (nextLeft != null && discardedLeft != null) {
						discardedLeft.push(nextLeft);
						nextLeft = null;
					}
					if (nextRight != null && discardedRight != null) {
						discardedRight.push(nextRight);
						nextRight = null;
					}

					if (discardedLeft != null) {

						while (leftIterator.hasNext()) {
							discardedLeft.push(leftIterator.next());
						}
						assert !leftIterator.hasNext() : leftIterator.toString();
					}

					if (discardedRight != null) {

						while (rightIterator.hasNext()) {
							discardedRight.push(rightIterator.next());
						}
						assert !rightIterator.hasNext() : rightIterator.toString();
					}

				}

			}

			@Override
			public void close() throws SailException {
				try {
					leftIterator.close();
				} finally {
					rightIterator.close();
				}
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			public ValidationTuple next() throws SailException {
				calculateNext();
				ValidationTuple temp = next;
				next = null;
				return temp;
			}

			@Override
			public void remove() throws SailException {
				throw new UnsupportedOperationException();
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
	public String toString() {
		return "InnerJoin(" + left.toString() + " : " + right.toString() + ")";
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

		if (discardedLeft != null) {
			discardedLeft.resetNotification();
		}
		if (discardedRight != null) {
			discardedRight.resetNotification();
		}

		while (true) {
			if (iterator.hasNext()) {
				ValidationTuple next = iterator.next();
				if (joined != null) {
					joined.push(next);
				}

				if (discardedRight != null) {
					if (!discardedRight.wasRecentlyPushed()) {
						continue;
					}
				}

				if (discardedLeft != null) {
					if (!discardedLeft.wasRecentlyPushed()) {
						continue;
					}
				}

				return true;
			} else {
				return false;
			}
		}

	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		Stream.of(joined, discardedLeft, discardedRight, left, right)
				.filter(Objects::nonNull)
				.forEach(p -> p.receiveLogger(validationExecutionLogger));
	}

	@Override
	public boolean producesSorted() {
		return true;
	}

	@Override
	public boolean requiresSorted() {
		return true;
	}

	class NotifyingPushablePlanNode implements PushablePlanNode {
		PushablePlanNode delegate;

		boolean recentlyPushed = false;

		public NotifyingPushablePlanNode(PushablePlanNode delegate) {
			this.delegate = delegate;
		}

		@Override
		public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
			return delegate.iterator();
		}

		@Override
		public int depth() {
			return delegate.depth();
		}

		@Override
		public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
			delegate.getPlanAsGraphvizDot(stringBuilder);
		}

		@Override
		public String getId() {
			return delegate.getId();
		}

		@Override
		public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
			delegate.receiveLogger(validationExecutionLogger);
		}

		@Override
		public boolean producesSorted() {
			return delegate.producesSorted();
		}

		@Override
		public boolean requiresSorted() {
			return delegate.requiresSorted();
		}

		@Override
		public void push(ValidationTuple tuple) {
			recentlyPushed = true;
			delegate.push(tuple);
		}

		@Override
		public boolean isClosed() {
			return delegate.isClosed();
		}

		public void resetNotification() {
			this.recentlyPushed = false;
		}

		public boolean wasRecentlyPushed() {
			return recentlyPushed;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			NotifyingPushablePlanNode that = (NotifyingPushablePlanNode) o;
			return delegate.equals(that.delegate);
		}

		@Override
		public int hashCode() {
			return Objects.hash(delegate);
		}
	}

}
