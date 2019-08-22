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
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;

public class BufferedPlanNode<T extends MultiStreamPlanNode & PlanNode> implements PushablePlanNode {
	private final Logger logger = LoggerFactory.getLogger(BufferedPlanNode.class);

	private final T parent;
	private final String name;

	private Queue<Tuple> buffer = new ArrayDeque<>();
	private boolean closed;
	private boolean printed;
	private ValidationExecutionLogger validationExecutionLogger;

	BufferedPlanNode(T parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

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
			}

			@Override
			public Tuple next() throws SailException {
				calculateNext();
				Tuple tuple = buffer.remove();
				if (GlobalValidationExecutionLogging.loggingEnabled) {
					validationExecutionLogger.log(depth(),
							parent.getClass().getSimpleName() + ":Buffered:" + name + ".next()", tuple, parent,
							getId());
				}
				return tuple;
			}

			@Override
			public void remove() throws SailException {

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
	public IteratorData getIteratorDataType() {
		return parent.getIteratorDataType();
	}

	@Override
	public void push(Tuple next) {
		buffer.add(next);
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public String toString() {
		return "BufferedPlanNode";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		if (this.validationExecutionLogger == null) {
			this.validationExecutionLogger = validationExecutionLogger;
			parent.receiveLogger(validationExecutionLogger);
		}
	}
}
