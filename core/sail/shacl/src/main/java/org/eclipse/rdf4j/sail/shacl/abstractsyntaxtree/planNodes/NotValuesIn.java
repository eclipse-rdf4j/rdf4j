/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

public class NotValuesIn implements PlanNode {

	private final PlanNode source;
	private final PlanNode notIn;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public NotValuesIn(PlanNode source, PlanNode notIn) {
		this.source = source;
		this.notIn = notIn;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final CloseableIteration<? extends ValidationTuple, SailException> sourceIterator = source.iterator();

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

				while (next == null && sourceIterator.hasNext()) {
					ValidationTuple temp = sourceIterator.next();
					if (!notInValueSet.contains(temp.getValue())) {
						next = temp;
					}

				}

			}

			@Override
			ValidationTuple loggingNext() throws SailException {
				calculateNext();
				ValidationTuple temp = next;
				next = null;
				return temp;
			}

			@Override
			boolean localHasNext() throws SailException {
				calculateNext();

				return next != null;
			}

			@Override
			public void close() throws SailException {

				sourceIterator.close();
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}

	@Override
	public int depth() {
		return source.depth() + 1;
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
				"source=" + source +
				", notIn=" + notIn +
				'}';
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		source.receiveLogger(validationExecutionLogger);
		notIn.receiveLogger(validationExecutionLogger);
	}
}
