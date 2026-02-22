/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.Iterator;
import java.util.function.Function;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.explanation.TelemetryMetricNames;

public class BindingSetAssignmentQueryEvaluationStep implements QueryEvaluationStep {
	private final BindingSetAssignment node;
	private final Function<BindingSet, MutableBindingSet> bsMaker;

	public BindingSetAssignmentQueryEvaluationStep(BindingSetAssignment node, QueryEvaluationContext context) {
		this.node = node;
		bsMaker = context::createBindingSet;
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		final Iterator<BindingSet> assignments = node.getBindingSets().iterator();
		CloseableIteration<BindingSet> result;
		if (bindings.isEmpty()) {
			// we can just return the assignments directly without checking existing bindings
			result = new CloseableIteratorIteration<>(assignments);
		} else {
			// we need to verify that new binding assignments do not overwrite existing bindings
			result = new LookAheadIteration<>() {

				@Override
				protected BindingSet getNextElement() throws QueryEvaluationException {
					MutableBindingSet nextResult = null;
					while (nextResult == null && assignments.hasNext()) {
						final BindingSet assignedBindings = assignments.next();

						for (String name : assignedBindings.getBindingNames()) {
							if (nextResult == null) {
								nextResult = bsMaker.apply(bindings);
							}

							final Value assignedValue = assignedBindings.getValue(name);
							if (assignedValue != null) {
								// check that the binding assignment does not overwrite existing bindings.
								Value existingValue = bindings.getValue(name);
								if (existingValue == null || assignedValue.equals(existingValue)) {
									if (existingValue == null) {
										// we are not overwriting an existing binding.
										nextResult.addBinding(name, assignedValue);
									}
								} else {
									// if values are not equal there is no compatible merge and we should return no
									// next element.
									nextResult = null;
									break;
								}
							}
						}
					}
					return nextResult;
				}

				@Override
				protected void handleClose() {

				}
			};
		}

		return new IterationWrapper<>(result) {
			private long providedBindings;

			@Override
			public BindingSet next() throws QueryEvaluationException {
				BindingSet next = super.next();
				providedBindings++;
				return next;
			}

			@Override
			protected void handleClose() {
				try {
					node.setLongMetricActual(TelemetryMetricNames.BINDINGS_PROVIDED_ACTUAL,
							Math.max(0L, node.getLongMetricActual(TelemetryMetricNames.BINDINGS_PROVIDED_ACTUAL))
									+ providedBindings);
				} finally {
					super.handleClose();
				}
			}
		};
	}
}
