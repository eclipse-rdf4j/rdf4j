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

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BufferedPlanNode<T extends MultiStreamPlanNode & PlanNode> implements PushablePlanNode {
	private final Logger logger = LoggerFactory.getLogger(BufferedPlanNode.class);

	private final T parent;
	private final String name;

	private final Queue<ValidationTuple> buffer = new ArrayDeque<>();
	private boolean closed;
	private boolean printed;
	private ValidationExecutionLogger validationExecutionLogger;

	BufferedPlanNode(T parent, String name) {
		this.parent = parent;
		this.name = Objects.requireNonNull(name);
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
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
				return !buffer.isEmpty();
			}

			private void calculateNext() {

				while (buffer.isEmpty()) {
					boolean success = parent.incrementIterator();
					if (!success) {
						break;
					}
				}

				assert !buffer.isEmpty() || !parent.incrementIterator() && buffer.isEmpty();

			}

			@Override
			public ValidationTuple next() throws SailException {
				calculateNext();
				ValidationTuple tuple = buffer.remove();
				if (validationExecutionLogger.isEnabled()) {
					validationExecutionLogger.log(depth(),
							parent.getClass().getSimpleName() + ":Buffered:" + name + ".next()", tuple, parent, getId(),
							null);
				}
				return tuple;
			}

			@Override
			public void remove() throws SailException {
				throw new UnsupportedOperationException();
			}

			@Override
			public String toString() {
				return "BufferedPlanNode-Iterator::" + parent.toString();
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
		buffer.add(next);
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public String toString() {
		return "BufferedPlanNode::" + parent.toString();
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
		return parent.requiresSorted();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		BufferedPlanNode<?> that = (BufferedPlanNode<?>) o;
		return parent.equals(that.parent) && name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent, name);
	}
}
