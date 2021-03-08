/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

/**
 * @author Håvard Ottestad
 */
public class UnionNode implements PlanNode {

	private StackTraceElement[] stackTrace;
	private final PlanNode[] nodes;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public UnionNode(PlanNode... nodes) {
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = PlanNodeHelper.handleSorting(this, nodes[i]);
		}

		this.nodes = nodes;
		// this.stackTrace = Thread.currentThread().getStackTrace();
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final List<CloseableIteration<? extends ValidationTuple, SailException>> iterators = Arrays.stream(nodes)
					.map(PlanNode::iterator)
					.collect(Collectors.toList());

			final ValidationTuple[] peekList = new ValidationTuple[nodes.length];

			ValidationTuple next;
			ValidationTuple prev;

			private void calculateNext() {

				if (next != null) {
					return;
				}

				for (int i = 0; i < peekList.length; i++) {
					if (peekList[i] == null) {
						CloseableIteration<? extends ValidationTuple, SailException> iterator = iterators.get(i);
						if (iterator.hasNext()) {
							peekList[i] = iterator.next();
						}
					}
				}

				ValidationTuple sortedFirst = null;
				int sortedFirstIndex = -1;

				for (int i = 0; i < peekList.length; i++) {
					if (peekList[i] == null) {
						continue;
					}

					if (sortedFirst == null) {
						sortedFirst = peekList[i];
						sortedFirstIndex = i;
					} else {
						if (peekList[i].compareActiveTarget(sortedFirst) < 0) {
							sortedFirst = peekList[i];
							sortedFirstIndex = i;
						}
					}

				}

				if (sortedFirstIndex >= 0) {
					peekList[sortedFirstIndex] = null;
				}

				next = sortedFirst;
			}

			@Override
			public void close() throws SailException {
				iterators.forEach(CloseableIteration::close);
			}

			@Override
			protected boolean localHasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			protected ValidationTuple loggingNext() throws SailException {
				calculateNext();

				assert !(prev != null && next.compareActiveTarget(prev) < 0);

				ValidationTuple temp = next;
				prev = next;
				next = null;
				return temp;
			}

		};
	}

	@Override
	public int depth() {
		return Arrays.stream(nodes).mapToInt(PlanNode::depth).max().orElse(0) + 1;

	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");
		for (PlanNode node : nodes) {
			stringBuilder.append(node.getId() + " -> " + getId()).append("\n");
			node.getPlanAsGraphvizDot(stringBuilder);

		}
	}

	@Override
	public String toString() {
		return "UnionNode";
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		for (PlanNode node : nodes) {
			node.receiveLogger(validationExecutionLogger);
		}
	}

	@Override
	public boolean producesSorted() {
		return true;
	}

	@Override
	public boolean requiresSorted() {
		return true;
	}
}
