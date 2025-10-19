/*
 * ******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * ******************************************************************************
 */
package org.eclipse.rdf4j.sail.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Comparator;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.Test;

/**
 * Reproduces a mismatch where one dataset reports a value comparator and the other doesn't. The union should not
 * assert; it should degrade by reporting null and avoid ordered merging.
 */
public class UnionSailDatasetComparatorTest {

	private static class StubDataset implements SailDataset {
		private final Comparator<Value> cmp;

		StubDataset(Comparator<Value> cmp) {
			this.cmp = cmp;
		}

		@Override
		public void close() {
		}

		@Override
		public CloseableIteration<? extends Namespace> getNamespaces() {
			return new EmptyIteration<>();
		}

		@Override
		public String getNamespace(String prefix) {
			return null;
		}

		@Override
		public CloseableIteration<? extends Resource> getContextIDs() {
			return new EmptyIteration<>();
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource s, IRI p, Value o, Resource... c) {
			return new EmptyIteration<>();
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(StatementOrder order, Resource s, IRI p, Value o,
				Resource... c) {
			return new EmptyIteration<>();
		}

		@Override
		public CloseableIteration<? extends Triple> getTriples(Resource s, IRI p, Value o) {
			return new EmptyIteration<>();
		}

		@Override
		public Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts) {
			return Set.of();
		}

		@Override
		public Comparator<Value> getComparator() {
			return cmp;
		}
	}

	@Test
	public void mismatchedComparators_returnsNullAndNoAssert() throws QueryEvaluationException {
		SailDataset withComparator = new StubDataset(new ValueComparator());
		SailDataset withoutComparator = new StubDataset(null);

		SailDataset union = UnionSailDataset.getInstance(withComparator, withoutComparator);

		// Expect graceful degradation
		assertThat(union.getComparator()).isNull();

		// And ordered getStatements should not attempt to use a comparator in this case
		assertDoesNotThrow(() -> union.getStatements(StatementOrder.S, null, null, null));
	}
}
