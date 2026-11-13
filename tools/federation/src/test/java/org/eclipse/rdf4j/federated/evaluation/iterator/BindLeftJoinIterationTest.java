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
package org.eclipse.rdf4j.federated.evaluation.iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BindLeftJoinIterationTest {

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void identicalInputMappingsRetainEveryOccurrence(boolean matches) {
		var drug = Values.iri("urn:drug");
		var weight = Values.literal(200);
		var first = new ListBindingSet(List.of("drug"), drug);
		var second = new ListBindingSet(List.of("drug"), drug);
		var names = List.of(BoundJoinVALUESConversionIteration.INDEX_BINDING_NAME, "weight");
		var candidates = List.<BindingSet>of(
				new ListBindingSet(names, Values.literal(0), weight),
				new ListBindingSet(names, Values.literal(1), weight));
		BindingSet expected = matches ? new ListBindingSet(List.of("drug", "weight"), drug, weight) : first;

		try (var results = new BindLeftJoinIteration(new CloseableIteratorIteration<>(candidates.iterator()),
				List.of(first, second), row -> matches)) {
			assertEquals(List.of(expected, expected), Iterations.asList(results));
		}
	}

	@Test
	void cancellationStopsBeforeReadingCandidates() {
		@SuppressWarnings("unchecked")
		CloseableIteration<BindingSet> input = mock(CloseableIteration.class);
		try (var results = new BindLeftJoinIteration(input, List.of())) {
			assertTrue(results.requestCancellation());
			assertThrows(QueryInterruptedException.class, results::hasNext);
			verifyNoInteractions(input);
		}
	}

	@Test
	void cancellationStopsWhileSkippingRejectedCandidates() {
		@SuppressWarnings("unchecked")
		CloseableIteration<BindingSet> input = mock(CloseableIteration.class);
		when(input.hasNext()).thenReturn(true, true, false);
		when(input.next()).thenReturn(new ListBindingSet(
				List.of(BoundJoinVALUESConversionIteration.INDEX_BINDING_NAME), Values.literal(0)));
		var owner = new AtomicReference<BindLeftJoinIteration>();
		try (var results = new BindLeftJoinIteration(input, List.of(EmptyBindingSet.getInstance()), row -> {
			assertTrue(owner.get().requestCancellation());
			return false;
		})) {
			owner.set(results);
			assertThrows(QueryInterruptedException.class, results::hasNext);
			verify(input).next();
		}
		verify(input).close();
	}
}
