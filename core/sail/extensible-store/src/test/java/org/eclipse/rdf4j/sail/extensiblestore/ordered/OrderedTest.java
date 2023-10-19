/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.extensiblestore.ordered;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.ordering.StatementOrder;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.junit.jupiter.api.Test;

public class OrderedTest {

	public static final String NAMESPACE = "http://example.com/";

	@Test
	public void test() {
		ExtensibleStoreOrderedImplForTests store = new ExtensibleStoreOrderedImplForTests();

		try (NotifyingSailConnection connection = store.getConnection()) {
			connection.begin();
			connection.addStatement(Values.iri(NAMESPACE, "d"), RDFS.LABEL, Values.literal("b"));
			connection.addStatement(Values.iri(NAMESPACE, "e"), RDFS.LABEL, Values.literal("a"));
			connection.addStatement(Values.iri(NAMESPACE, "a"), RDFS.LABEL, Values.literal("e"));
			connection.addStatement(Values.iri(NAMESPACE, "c"), RDFS.LABEL, Values.literal("c"));
			connection.addStatement(Values.iri(NAMESPACE, "b"), RDFS.LABEL, Values.literal("d"));

			connection.commit();

			connection.begin(IsolationLevels.NONE);
			try (CloseableIteration<? extends Statement> statements = connection.getStatements(StatementOrder.S, null,
					null, null, true)) {
				List<? extends Statement> collect = statements.stream().collect(Collectors.toList());

				List<String> subjects = collect
						.stream()
						.map(Statement::getSubject)
						.map(i -> (IRI) i)
						.map(IRI::getLocalName)
						.collect(Collectors.toList());

				Assertions.assertThat(subjects).isEqualTo(List.of("a", "b", "c", "d", "e"));
			}

			connection.commit();
		}
	}

	@Test
	public void testReadCommitted() {
		ExtensibleStoreOrderedImplForTests store = new ExtensibleStoreOrderedImplForTests();

		try (NotifyingSailConnection connection = store.getConnection()) {
			connection.begin();
			connection.addStatement(Values.iri(NAMESPACE, "a"), RDFS.LABEL, Values.literal("e"));
			connection.addStatement(Values.iri(NAMESPACE, "b"), RDFS.LABEL, Values.literal("d"));
			connection.addStatement(Values.iri(NAMESPACE, "c"), RDFS.LABEL, Values.literal("c"));
			connection.addStatement(Values.iri(NAMESPACE, "d"), RDFS.LABEL, Values.literal("b"));
			connection.addStatement(Values.iri(NAMESPACE, "e"), RDFS.LABEL, Values.literal("a"));
			connection.commit();

			connection.begin(IsolationLevels.READ_COMMITTED);
			try (CloseableIteration<? extends Statement> statements = connection.getStatements(StatementOrder.S, null,
					null, null, true)) {
				List<? extends Statement> collect = statements.stream().collect(Collectors.toList());

				List<String> subjects = collect
						.stream()
						.map(Statement::getSubject)
						.map(i -> (IRI) i)
						.map(IRI::getLocalName)
						.collect(Collectors.toList());

				Assertions.assertThat(subjects).isEqualTo(List.of("a", "b", "c", "d", "e"));
			}

			connection.commit();
		}
	}

}
