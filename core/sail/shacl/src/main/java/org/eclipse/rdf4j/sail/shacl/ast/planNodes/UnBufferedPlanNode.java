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

public class UnBufferedPlanNode<T extends PlanNode & MultiStreamPlanNode> implements PushablePlanNode {
	private final Logger logger = LoggerFactory.getLogger(UnBufferedPlanNode.class);

	private final T parent;

	ValidationTuple next;
	private boolean closed;
	private boolean printed;

	String name;
	private ValidationExecutionLogger validationExecutionLogger;

	UnBufferedPlanNode(T parent, String name) {
		this.parent = parent;
		this.name = Objects.requireNonNull(name);
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		next = null;
		closed = false;

		return new CloseableIteration<>() {

			{
				parent.init();
			}

			@Override
			public void close() throws SailException {
				closed = true;
				parent.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			private void calculateNext() {
				while (next == null) {
					boolean success = parent.incrementIterator();
					if (!success) {
						break;
					}
				}

				assert next != null || (!parent.incrementIterator() && next == null);
			}

			@Override
			public ValidationTuple next() throws SailException {
				calculateNext();
				ValidationTuple tuple = next;
				if (validationExecutionLogger.isEnabled()) {
					validationExecutionLogger.log(depth(),
							parent.getClass().getSimpleName() + ":UnBuffered" + name + ".next()", tuple, parent,
							getId(), null);
				}
				next = null;

				return tuple;
			}

			@Override
			public void remove() throws SailException {
				throw new UnsupportedOperationException();
			}

		};
	}

	@Override
	public int depth() {
		return parent.depth();
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		parent.getPlanAsGraphvizDot(stringBuilder);

		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public void push(ValidationTuple next) {
		this.next = next;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public String toString() {
		return "UnBufferedPlanNode(" + parent.getClass().getSimpleName() + ")";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		if (this.validationExecutionLogger == null) {
			this.validationExecutionLogger = validationExecutionLogger;
			parent.receiveLogger(validationExecutionLogger);
		}
	}

	@Override
	public boolean producesSorted() {
		return parent.producesSorted();
	}

	@Override
	public boolean requiresSorted() {
		return parent.producesSorted();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		UnBufferedPlanNode<?> that = (UnBufferedPlanNode<?>) o;
		return parent.equals(that.parent) && name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent, name);
	}
}
