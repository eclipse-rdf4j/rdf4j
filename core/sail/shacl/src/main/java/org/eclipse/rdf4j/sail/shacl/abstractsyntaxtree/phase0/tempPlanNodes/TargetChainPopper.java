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
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.planNodes.ValidationExecutionLogger;

/**
 * @author Håvard Ottestad
 */
public class TargetChainPopper implements TupleValidationPlanNode {

	private final TupleValidationPlanNode parent;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public TargetChainPopper(TupleValidationPlanNode parent) {
		this.parent = parent;
	}

	@Override
	public CloseableIteration<ValidationTuple, SailException> iterator() {

		return new LoggingCloseableValidationIteration(this, validationExecutionLogger) {

			final private CloseableIteration<ValidationTuple, SailException> iterator = parent.iterator();

			@Override
			public void close() throws SailException {
				iterator.close();
			}

			@Override
			boolean localHasNext() throws SailException {
				return iterator.hasNext();
			}

			@Override
			ValidationTuple loggingNext() throws SailException {
				ValidationTuple next = iterator.next();

				Value value = next.getTargetChain().removeLast();
				next.setValue(value);

				return next;
			}

			@Override
			public void remove() throws SailException {
				iterator.remove();
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
		return "UnionNode";
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
