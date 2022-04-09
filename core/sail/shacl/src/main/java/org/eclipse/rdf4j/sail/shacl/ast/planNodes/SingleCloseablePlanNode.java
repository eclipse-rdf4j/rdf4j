/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A plan node that can only be closed once
 *
 *
 * @author Håvard Ottestad
 */
public class SingleCloseablePlanNode implements PlanNode {

	private final PlanNode parent;

	public SingleCloseablePlanNode(PlanNode parent) {
		this.parent = PlanNodeHelper.handleSorting(this, parent);
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new SingleCloseableIteration(parent);
	}

	@Override
	public int depth() {
		throw new IllegalStateException();
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String toString() {
		return "SingleCloseablePlanNode{" + "parent=" + parent + '}';
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
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
		SingleCloseablePlanNode that = (SingleCloseablePlanNode) o;
		return parent.equals(that.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent);
	}

	private static class SingleCloseableIteration implements CloseableIteration<ValidationTuple, SailException> {

		final CloseableIteration<? extends ValidationTuple, SailException> parentIterator;
		final AtomicBoolean closed = new AtomicBoolean(false);

		public SingleCloseableIteration(PlanNode parent) {
			parentIterator = parent.iterator();
		}

		@Override
		public void close() throws SailException {
			if (closed.compareAndSet(false, true)) {
				parentIterator.close();
			}
		}

		@Override
		public boolean hasNext() throws SailException {
			return parentIterator.hasNext();
		}

		@Override
		public ValidationTuple next() throws SailException {
			return parentIterator.next();
		}

		@Override
		public void remove() throws SailException {
			parentIterator.remove();
		}
	}
}
