/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.join;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.evaluation.FederationEvaluationStrategy;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.junit.jupiter.api.Test;

class ParallelLeftJoinTaskTest {

	@Test
	void rejectedCandidateAfterMatchDoesNotReturnUnmatchedRow() {
		var strategy = mock(FederationEvaluationStrategy.class);
		var drug = Values.iri("urn:drug");
		var left = new ListBindingSet(List.of("drug"), drug);
		var accepted = new ListBindingSet(List.of("drug", "weight"), drug, Values.literal(200));
		var rejected = new ListBindingSet(List.of("drug", "weight"), drug, Values.literal(100));
		var right = new StatementPattern(Var.of("drug"), Var.of("predicate"), Var.of("weight"));
		var condition = new Compare(Var.of("weight"), new ValueConstant(Values.literal(114)), Compare.CompareOp.GT);
		var join = new LeftJoin(new SingletonSet(), right, condition);

		when(strategy.evaluate(right, left))
				.thenReturn(new CloseableIteratorIteration<>(List.<BindingSet>of(accepted, rejected).iterator()));
		when(strategy.isTrue(condition, accepted)).thenReturn(true);

		try (var iteration = new ParallelLeftJoinTask.FedXLeftJoinIteration(strategy, join, left)) {
			assertEquals(List.of(accepted), Iterations.asList(iteration));
		}
	}
}
