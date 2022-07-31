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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public abstract class FilterPlanNode implements MultiStreamPlanNode, PlanNode {

	static private final Logger logger = LoggerFactory.getLogger(FilterPlanNode.class);

	PlanNode parent;

	private PushablePlanNode trueNode;
	private PushablePlanNode falseNode;

	private CloseableIteration<ValidationTuple, SailException> iterator;
	private ValidationExecutionLogger validationExecutionLogger;

	abstract boolean checkTuple(ValidationTuple t);

	public FilterPlanNode(PlanNode parent) {
		this.parent = PlanNodeHelper.handleSorting(this, parent);
	}

	public PlanNode getTrueNode(Class<? extends PushablePlanNode> type) {
		if (trueNode != null) {
			throw new IllegalStateException();
		}
		if (type == BufferedPlanNode.class) {
			trueNode = new BufferedPlanNode<>(this, "True");
		} else {
			trueNode = new UnBufferedPlanNode<>(this, "True");

		}

		return trueNode;
	}

	public PlanNode getFalseNode(Class<? extends PushablePlanNode> type) {
		if (falseNode != null) {
			throw new IllegalStateException();
		}
		if (type == BufferedPlanNode.class) {
			falseNode = new BufferedPlanNode<>(this, "False");
		} else {
			falseNode = new UnBufferedPlanNode<>(this, "False");

		}

		return falseNode;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		throw new IllegalStateException("Must specify if filter should return false or true nodes!");
	}

	private CloseableIteration<ValidationTuple, SailException> iteratorInternal() {

		return new CloseableIteration<>() {

			CloseableIteration<? extends ValidationTuple, SailException> parentIterator;

			ValidationTuple next;

			private void calculateNext() {
				if (parentIterator == null) {
					parentIterator = parent.iterator();
				}

				if (next != null) {
					return;
				}

				while (parentIterator.hasNext() && next == null) {
					ValidationTuple temp = parentIterator.next();

					if (checkTuple(temp)) {
						if (trueNode != null) {
							trueNode.push(temp);
						} else {
							if (validationExecutionLogger.isEnabled()) {
								validationExecutionLogger.log(FilterPlanNode.this.depth(),
										FilterPlanNode.this.getClass().getSimpleName() + ":IgnoredAsTrue.next()", temp,
										FilterPlanNode.this, getId(), null);
							}
						}
					} else {
						if (falseNode != null) {
							falseNode.push(temp);
						} else {
							if (validationExecutionLogger.isEnabled()) {
								validationExecutionLogger.log(FilterPlanNode.this.depth(),
										FilterPlanNode.this.getClass().getSimpleName() + ":IgnoredAsFalse.next()", temp,
										FilterPlanNode.this, getId(), null);
							}
						}
					}

					next = temp;

				}

			}

			boolean closed = false;

			@Override
			public void close() throws SailException {
				if (closed) {
					return;
				}

				closed = true;
				if (parentIterator != null) {
					parentIterator.close();
				}
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			public ValidationTuple next() throws SailException {
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

	boolean printed = false;

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");
		stringBuilder.append(parent.getId() + " -> " + getId()).append("\n");
		if (trueNode != null) {
			stringBuilder.append(getId() + " -> " + trueNode.getId() + " [label=\"true values\"]").append("\n");

		}
		if (falseNode != null) {
			stringBuilder.append(getId() + " -> " + falseNode.getId() + " [label=\"false values\"]").append("\n");

		}

		parent.getPlanAsGraphvizDot(stringBuilder);

	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public void init() {
		if (iterator == null) {
			iterator = iteratorInternal();
		}
	}

	@Override
	public void close() {
		if ((trueNode == null || trueNode.isClosed()) && (falseNode == null || falseNode.isClosed())) {
			iterator.close();
			iterator = null;
		}

	}

	@Override
	public boolean incrementIterator() {
		if (iterator.hasNext()) {
			iterator.next();
			return true;
		}
		return false;
	}

	@Override
	public int depth() {
		return parent.depth() + 1;
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parent.receiveLogger(validationExecutionLogger);
	}

	@Override
	public boolean producesSorted() {
		return parent.producesSorted();
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FilterPlanNode that = (FilterPlanNode) o;
		return parent.equals(that.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent);
	}

}
