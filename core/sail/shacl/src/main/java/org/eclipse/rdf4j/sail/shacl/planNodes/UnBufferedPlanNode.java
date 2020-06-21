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

public class UnBufferedPlanNode<T extends PlanNode & MultiStreamPlanNode> implements PushablePlanNode {
	private final Logger logger = LoggerFactory.getLogger(UnBufferedPlanNode.class);

	private T parent;

	Tuple next;
	private boolean closed;
	private boolean printed;

	String name;
	private ValidationExecutionLogger validationExecutionLogger;

	UnBufferedPlanNode(T parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		next = null;
		closed = false;

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
				return next != null;
			}

			private void calculateNext() {
				while (next == null) {
					boolean success = parent.incrementIterator();
					if (!success) {
						break;
					}
				}
			}

			@Override
			public Tuple next() throws SailException {
				calculateNext();
				Tuple tuple = next;
				if (GlobalValidationExecutionLogging.loggingEnabled) {
					validationExecutionLogger.log(depth(),
							parent.getClass().getSimpleName() + ":UnBuffered" + name + ".next()", tuple, parent,
							getId());
				}
				next = null;

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
		this.next = next;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public String toString() {
		return "UnBufferedPlanNode";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		if (this.validationExecutionLogger == null) {
			this.validationExecutionLogger = validationExecutionLogger;
			parent.receiveLogger(validationExecutionLogger);
		}
	}
}
