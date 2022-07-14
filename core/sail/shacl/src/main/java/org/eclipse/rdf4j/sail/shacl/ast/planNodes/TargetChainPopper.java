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
import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Pops the last target off of the target chain and into the value.
 *
 * This is useful when a plan node operates on the values, but tuple with only targets is supplied and we want to
 * validate the last target.
 *
 *
 * @author Håvard Ottestad
 */
public class TargetChainPopper implements PlanNode {

	private final PlanNode parent;
	private StackTraceElement[] stackTrace;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public TargetChainPopper(PlanNode parent) {
		this.parent = PlanNodeHelper.handleSorting(this, parent);
		// this.stackTrace = Thread.currentThread().getStackTrace();
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final private CloseableIteration<? extends ValidationTuple, SailException> parentIterator = parent
					.iterator();
			Iterator<ValidationTuple> iterator = Collections.emptyIterator();

			public void calculateNext() {
				if (!iterator.hasNext()) {
					if (parentIterator.hasNext()) {
						List<ValidationTuple> validationTuples = parentIterator.next().pop();
						iterator = validationTuples.iterator();
					}
				}

			}

			@Override
			public void localClose() throws SailException {
				parentIterator.close();
				iterator = Collections.emptyIterator();
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
		return "TargetChainPopper";
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TargetChainPopper that = (TargetChainPopper) o;
		return parent.equals(that.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent);
	}
}
