/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.function.Consumer;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Used for adding a custom log statement to tuples as they pass through. Should only be used for debugging.
 */
public class DebugPlanNode implements PlanNode {

	private StackTraceElement[] stackTrace;
	private Consumer<ValidationTuple> debugPoint;
	private final String message;
	PlanNode parent;
	private boolean printed;
	private ValidationExecutionLogger validationExecutionLogger;

	public DebugPlanNode(PlanNode parent, String message, Consumer<ValidationTuple> debugPoint) {
		this(parent, message);
		this.debugPoint = debugPoint;
	}

	public DebugPlanNode(PlanNode parent, String message) {
		this.parent = parent;
		this.message = message;
		// this.stackTrace = Thread.currentThread().getStackTrace();

	}

	public DebugPlanNode(PlanNode parent, Consumer<ValidationTuple> debugPoint) {
		this(parent, (String) null);
		this.debugPoint = debugPoint;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		if (validationExecutionLogger == null && validationExecutionLogger.isEnabled()) {
			throw new IllegalStateException("Did not receive validationExecutionLogger before .iterator() was called!");
		}

		return new CloseableIteration<>() {

			final CloseableIteration<? extends ValidationTuple, SailException> iterator = parent.iterator();

			@Override
			public boolean hasNext() throws SailException {
				return iterator.hasNext();
			}

			@Override
			public ValidationTuple next() throws SailException {
				ValidationTuple next = iterator.next();
				if (debugPoint != null) {
					debugPoint.accept(next);
				}

				if (message != null && validationExecutionLogger.isEnabled()) {
					validationExecutionLogger.log(depth(), message, next, DebugPlanNode.this, getId(), null);
				}
				return next;
			}

			@Override
			public void remove() throws SailException {
				iterator.remove();
			}

			@Override
			public void close() throws SailException {
				iterator.close();
			}
		};

	}

	@Override
	public int depth() {
		return parent.depth() + 1;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");
		stringBuilder.append(parent.getId() + " -> " + getId()).append("\n");
		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public String toString() {
		return "DebugPlanNode";
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
}
