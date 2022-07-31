/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationExecutionLogger;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;

/**
 * @author Håvard Ottestad
 */
public class MockInputPlanNode implements PlanNode {
	public static final Resource[] CONTEXTS = { null };

	Collection<ValidationTuple> initialData;
	private ValidationExecutionLogger validationExecutionLogger;

	public MockInputPlanNode(Collection<ValidationTuple> initialData) {
		this.initialData = initialData;
	}

	@SafeVarargs
	public MockInputPlanNode(Collection<String>... list) {

		initialData = Arrays.stream(list)
				.map(strings -> strings.stream()
						.map(SimpleValueFactory.getInstance()::createLiteral)
						.map(l -> (Value) l)
						.collect(Collectors.toList()))
				.map(v -> {
					if (v.size() > 1) {
						return new ValidationTuple(new ArrayList<>(v), ConstraintComponent.Scope.propertyShape, true,
								CONTEXTS);
					} else {
						return new ValidationTuple(new ArrayList<>(v), ConstraintComponent.Scope.propertyShape, false,
								CONTEXTS);
					}
				})
				.sorted(ValidationTuple::compareValue)
				.sorted(ValidationTuple::compareFullTarget)
				.collect(Collectors.toList());

	}

	@Override
	public CloseableIteration<ValidationTuple, SailException> iterator() {
		return new CloseableIteration<>() {

			final Iterator<ValidationTuple> iterator = initialData.iterator();

			@Override
			public void close() throws SailException {
			}

			@Override
			public boolean hasNext() throws SailException {
				return iterator.hasNext();
			}

			@Override
			public ValidationTuple next() throws SailException {
				return iterator.next();
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}

	@Override
	public int depth() {
		return 0;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {

	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		if (this.validationExecutionLogger == null) {
			this.validationExecutionLogger = validationExecutionLogger;
		}
	}

	@Override
	public boolean producesSorted() {
		return true;
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}

}
