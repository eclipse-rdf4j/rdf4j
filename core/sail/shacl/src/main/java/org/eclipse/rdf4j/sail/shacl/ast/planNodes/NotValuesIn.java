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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

public class NotValuesIn implements PlanNode {

	private final PlanNode parent;
	private final PlanNode notIn;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public NotValuesIn(PlanNode parent, PlanNode notIn) {
		this.parent = PlanNodeHelper.handleSorting(this, parent);
		this.notIn = notIn;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final CloseableIteration<? extends ValidationTuple, SailException> parentIterator = parent.iterator();

			final Set<Value> notInValueSet = new HashSet<>();

			{
				try (CloseableIteration<? extends ValidationTuple, SailException> iterator = notIn.iterator()) {
					while (iterator.hasNext()) {
						notInValueSet.add(iterator.next().getValue());
					}
				}
			}

			ValidationTuple next;

			void calculateNext() {

				while (next == null && parentIterator.hasNext()) {
					ValidationTuple temp = parentIterator.next();
					if (!notInValueSet.contains(temp.getValue())) {
						next = temp;
					}

				}

			}

			@Override
			protected ValidationTuple loggingNext() throws SailException {
				calculateNext();
				ValidationTuple temp = next;
				next = null;
				return temp;
			}

			@Override
			protected boolean localHasNext() throws SailException {
				calculateNext();

				return next != null;
			}

			@Override
			public void localClose() throws SailException {
				parentIterator.close();
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
		stringBuilder.append(getId())
				.append(" [label=\"")
				.append(StringEscapeUtils.escapeJava(this.toString()))
				.append("\"];")
				.append("\n");
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public String toString() {
		return "NotValuesIn{" +
				"parent=" + parent +
				", notIn=" + notIn +
				'}';
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parent.receiveLogger(validationExecutionLogger);
		notIn.receiveLogger(validationExecutionLogger);
	}

	@Override
	public boolean producesSorted() {
		return true;
	}

	@Override
	public boolean requiresSorted() {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NotValuesIn that = (NotValuesIn) o;
		return parent.equals(that.parent) && notIn.equals(that.notIn);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent, notIn);
	}
}
