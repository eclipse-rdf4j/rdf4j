/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
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
	public CloseableIteration<Tuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();

			Tuple tempNext;

			Tuple next;

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

				next = new Tuple();

				Value subject = tempNext.getLine().get(0);

				while (tempNext != null
						&& (tempNext.getLine().get(0) == subject || tempNext.getLine().get(0).equals(subject))) {

					next.addHistory(tempNext);
					if (tempNext.getLine().size() > 1) {
						count++;
					}

					if (parentIterator.hasNext()) {
						tempNext = parentIterator.next();
					} else {
						tempNext = null;
					}

				}

				// Arrays.asList(...) is immutable, wrap in ArrayList to make it mutable
				List<Value> line = new ArrayList<>(
						Arrays.asList(subject, SimpleValueFactory.getInstance().createLiteral(count)));

				next.setLine(line);

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
			Tuple loggingNext() throws SailException {

				calculateNext();

				Tuple temp = next;
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
	public IteratorData getIteratorDataType() {
		return IteratorData.aggregated;
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parent.receiveLogger(validationExecutionLogger);
	}
}
