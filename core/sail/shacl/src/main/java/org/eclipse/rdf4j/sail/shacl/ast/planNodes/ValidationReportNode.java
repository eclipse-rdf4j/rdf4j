/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;

public class ValidationReportNode implements PlanNode {

	private final Function<ValidationTuple, ValidationResult> validationResultFunction;
	PlanNode parent;
	private boolean printed = false;

	public ValidationReportNode(PlanNode parent,
			Function<ValidationTuple, ValidationResult> validationResultFunction) {
		parent = PlanNodeHelper.handleSorting(this, parent);
		this.parent = parent;
		this.validationResultFunction = validationResultFunction;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		return new CloseableIteration<ValidationTuple, SailException>() {

			private final CloseableIteration<? extends ValidationTuple, SailException> iterator = parent.iterator();

			@Override
			public void close() throws SailException {
				iterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				return iterator.hasNext();
			}

			@Override
			public ValidationTuple next() throws SailException {
				ValidationTuple next = iterator.next();
				return next.addValidationResult(validationResultFunction.apply(next));
			}

			@Override
			public void remove() throws SailException {
				iterator.remove();
			}
		};
	}

	@Override
	public int depth() {
		return parent.depth();
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
		return "ValidationReportNode";
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
}
