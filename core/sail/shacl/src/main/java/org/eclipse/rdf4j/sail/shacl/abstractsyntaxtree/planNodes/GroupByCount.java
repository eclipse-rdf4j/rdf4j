/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailException;

/**
 * @author Håvard Ottestad
 */
public class GroupByCount implements PlanNode {

	PlanNode parent;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public GroupByCount(PlanNode parent) {
		this.parent = parent;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final CloseableIteration<? extends ValidationTuple, SailException> parentIterator = parent.iterator();

			ValidationTuple tempNext;

			AggregatedValidationTuple next;

			private void calculateNext() {
				if (next != null) {
					return;
				}

				if (tempNext == null && parentIterator.hasNext()) {
					tempNext = parentIterator.next();
				}

				if (tempNext == null) {
					return;
				}

				long count = 0;

				next = new AggregatedValidationTuple(tempNext);

				while (tempNext != null && tempNext.sameTargetAs(next)) {

					if (tempNext.hasValue()) {
						count++;
						next.addAggregate(tempNext.getValue());
					}

					if (parentIterator.hasNext()) {
						tempNext = parentIterator.next();
					} else {
						tempNext = null;
					}

				}

				next.getChain().addLast(SimpleValueFactory.getInstance().createLiteral(count));

			}

			@Override
			public void close() throws SailException {
				parentIterator.close();
			}

			@Override
			boolean localHasNext() throws SailException {
				calculateNext();

				return next != null;
			}

			@Override
			AggregatedValidationTuple loggingNext() throws SailException {

				calculateNext();

				AggregatedValidationTuple temp = next;
				next = null;

				return temp;
			}

			@Override
			public void remove() throws SailException {

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
		return "GroupByCount";
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
}
