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

import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;

public class ValidationReportNode implements PlanNode {

	private final Function<ValidationTuple, ValidationResult> validationResultFunction;
	PlanNode parent;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public ValidationReportNode(PlanNode parent, Function<ValidationTuple, ValidationResult> validationResultFunction) {
		this.parent = PlanNodeHelper.handleSorting(this, parent);
		this.validationResultFunction = validationResultFunction;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			private final CloseableIteration<? extends ValidationTuple, SailException> iterator = parent.iterator();

			@Override
			public void localClose() throws SailException {
				iterator.close();
			}

			@Override
			public boolean localHasNext() throws SailException {
				return iterator.hasNext();
			}

			@Override
			public ValidationTuple loggingNext() throws SailException {
				ValidationTuple next = iterator.next();
				return next.addValidationResult(validationResultFunction);
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
		return "ValidationReportNode";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
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
		ValidationReportNode that = (ValidationReportNode) o;
		return validationResultFunction.equals(that.validationResultFunction) && parent.equals(that.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(validationResultFunction, parent);
	}
}
