/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

/**
 * @author Håvard Ottestad
 */
public class ShiftToNodeShape implements PlanNode {

	private StackTraceElement[] stackTrace;
	PlanNode parent;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	boolean keepPath = false;

	public ShiftToNodeShape(PlanNode parent) {
		parent = PlanNodeHelper.handleSorting(this, parent);
		this.parent = parent;
		// this.stackTrace = Thread.currentThread().getStackTrace();
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final CloseableIteration<? extends ValidationTuple, SailException> parentIterator = parent.iterator();
			Iterator<ValidationTuple> iterator = Collections.emptyIterator();

			public void calculateNext() {
				if (!iterator.hasNext()) {
					if (parentIterator.hasNext()) {
						List<ValidationTuple> validationTuples = parentIterator.next().shiftToNodeShape();
						iterator = validationTuples.iterator();
					}
					assert iterator.hasNext() || !parentIterator.hasNext();
				}

			}

			@Override
			public void close() throws SailException {
				parentIterator.close();
			}

			@Override
			protected boolean localHasNext() throws SailException {
				calculateNext();
				return iterator.hasNext();
			}

			@Override
			protected ValidationTuple loggingNext() throws SailException {
				calculateNext();

				return iterator.next();
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
	public String toString() {
		return "TrimToTarget";
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parent.receiveLogger(validationExecutionLogger);
	}

	@Override
	public boolean producesSorted() {
		return false;
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}
}
