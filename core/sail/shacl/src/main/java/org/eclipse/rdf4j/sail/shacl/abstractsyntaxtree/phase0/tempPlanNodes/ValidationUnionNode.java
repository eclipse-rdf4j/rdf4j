/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.planNodes.ValidationExecutionLogger;

/**
 * @author Håvard Ottestad
 */
public class ValidationUnionNode implements TupleValidationPlanNode {

	private final TupleValidationPlanNode[] nodes;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public ValidationUnionNode(TupleValidationPlanNode... nodes) {
		this.nodes = nodes;
	}

	@Override
	public CloseableIteration<ValidationTuple, SailException> iterator() {
		return new LoggingCloseableValidationIteration(this, validationExecutionLogger) {

			final List<CloseableIteration<ValidationTuple, SailException>> iterators = Arrays.stream(nodes)
					.map(TupleValidationPlanNode::iterator)
					.collect(Collectors.toList());

			final ValidationTuple[] peekList = new ValidationTuple[nodes.length];

			ValidationTuple next;

			private void calculateNext() {

				if (next != null) {
					return;
				}

				for (int i = 0; i < peekList.length; i++) {
					if (peekList[i] == null) {
						CloseableIteration<ValidationTuple, SailException> iterator = iterators.get(i);
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
						if (peekList[i].compareTarget(sortedFirst) < 0) {
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
			boolean localHasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			ValidationTuple loggingNext() throws SailException {
				calculateNext();

				ValidationTuple temp = next;
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
		return Arrays.stream(nodes).mapToInt(TupleValidationPlanNode::depth).max().orElse(0) + 1;

	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");
		for (TupleValidationPlanNode node : nodes) {
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
		for (TupleValidationPlanNode node : nodes) {
			node.receiveLogger(validationExecutionLogger);
		}
	}
}
