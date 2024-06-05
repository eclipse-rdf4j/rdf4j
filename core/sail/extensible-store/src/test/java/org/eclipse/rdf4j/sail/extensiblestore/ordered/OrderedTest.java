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
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencerConnection;
import org.junit.jupiter.api.Test;

public class OrderedTest {

	public static final String NAMESPACE = "http://example.com/";

	@Test
	public void testSubject() {
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
	public void testObjectWithInferencer() {
		SchemaCachingRDFSInferencer schemaCachingRDFSInferencer = new SchemaCachingRDFSInferencer(
				new ExtensibleStoreOrderedImplForTests());

		try (SchemaCachingRDFSInferencerConnection connection = schemaCachingRDFSInferencer.getConnection()) {
			connection.begin();
			connection.addStatement(Values.iri(NAMESPACE, "d"), RDFS.LABEL, Values.literal("b"));
			connection.addInferredStatement(Values.iri(NAMESPACE, "e"), RDFS.LABEL, Values.literal("a"));
			connection.addInferredStatement(Values.iri(NAMESPACE, "a"), RDFS.LABEL, Values.literal("e"));
			connection.addStatement(Values.iri(NAMESPACE, "c"), RDFS.LABEL, Values.literal("c"));
			connection.addStatement(Values.iri(NAMESPACE, "b"), RDFS.LABEL, Values.literal("d"));
			connection.commit();

			connection.begin(IsolationLevels.NONE);
			try (CloseableIteration<? extends Statement> statements = connection.getStatements(StatementOrder.O, null,
					null, null, true)) {
				List<? extends Statement> collect = statements.stream().collect(Collectors.toList());

				List<String> subjects = collect
						.stream()
						.map(Statement::getObject)
						.filter(Literal.class::isInstance)
						.map(i -> (Literal) i)
						.map(Literal::getLabel)
						.collect(Collectors.toList());

				Assertions.assertThat(subjects).isEqualTo(List.of("a", "b", "c", "d", "e"));
			}

			connection.commit();
		}
	}

	@Test
	public void testSparql() {
		SailRepository store = new SailRepository(new ExtensibleStoreOrderedImplForTests());

		try (SailRepositoryConnection connection = store.getConnection()) {
			connection.begin();
			connection.add(Values.iri(NAMESPACE, "d"), RDFS.LABEL, Values.literal("b"));
			connection.add(Values.iri(NAMESPACE, "e"), RDFS.LABEL, Values.literal("a"));
			connection.add(Values.iri(NAMESPACE, "a"), RDFS.LABEL, Values.literal("e"));
			connection.add(Values.iri(NAMESPACE, "c"), RDFS.LABEL, Values.literal("c"));
			connection.add(Values.iri(NAMESPACE, "b"), RDFS.LABEL, Values.literal("d"));

			connection.add(Values.iri(NAMESPACE, "d"), RDFS.COMMENT, Values.literal("1b"));
			connection.add(Values.iri(NAMESPACE, "e"), RDFS.COMMENT, Values.literal("1a"));
			connection.add(Values.iri(NAMESPACE, "a"), RDFS.COMMENT, Values.literal("1e"));
			connection.add(Values.iri(NAMESPACE, "c"), RDFS.COMMENT, Values.literal("1c"));
			connection.add(Values.iri(NAMESPACE, "b"), RDFS.COMMENT, Values.literal("1d"));
			connection.add(Values.bnode(), RDFS.COMMENT, Values.literal("1d"));
			connection.add(Values.bnode(), RDFS.COMMENT, Values.literal("1d"));
			connection.add(Values.bnode(), RDFS.COMMENT, Values.literal("1d"));
			connection.add(Values.bnode(), RDFS.COMMENT, Values.literal("1d"));
			connection.add(Values.bnode(), RDFS.COMMENT, Values.literal("1d"));
			connection.add(Values.bnode(), RDFS.COMMENT, Values.literal("1d"));
			connection.add(Values.bnode(), RDFS.COMMENT, Values.literal("1d"));
			connection.add(Values.bnode(), RDFS.COMMENT, Values.literal("1d"));
			connection.add(Values.bnode(), RDFS.COMMENT, Values.literal("1d"));

			connection.add(Values.iri(NAMESPACE, "d"), FOAF.KNOWS, Values.iri(NAMESPACE, "b"));
			connection.add(Values.iri(NAMESPACE, "e"), FOAF.KNOWS, Values.iri(NAMESPACE, "d"));
			connection.add(Values.iri(NAMESPACE, "a"), FOAF.KNOWS, Values.iri(NAMESPACE, "e"));
			connection.add(Values.iri(NAMESPACE, "c"), FOAF.KNOWS, Values.iri(NAMESPACE, "a"));
			connection.add(Values.iri(NAMESPACE, "b"), FOAF.KNOWS, Values.iri(NAMESPACE, "c"));
			connection.add(Values.bnode(), FOAF.KNOWS, Values.bnode());
			connection.add(Values.bnode(), FOAF.KNOWS, Values.bnode());
			connection.add(Values.bnode(), FOAF.KNOWS, Values.bnode());
			connection.add(Values.bnode(), FOAF.KNOWS, Values.bnode());
			connection.add(Values.bnode(), FOAF.KNOWS, Values.bnode());
			connection.add(Values.bnode(), FOAF.KNOWS, Values.bnode());
			connection.add(Values.bnode(), FOAF.KNOWS, Values.bnode());
			connection.add(Values.bnode(), FOAF.KNOWS, Values.bnode());
			connection.add(Values.bnode(), FOAF.KNOWS, Values.bnode());

			connection.commit();

			connection.begin(IsolationLevels.NONE);

			String query = "SELECT * WHERE {\n" +
					"?s <" + RDFS.LABEL + "> ?o. \n" +
					"?s <" + RDFS.COMMENT + "> ?o2.\n" +
					"?s <" + FOAF.KNOWS + "> ?s2.\n" +
					"?s2 <" + RDFS.COMMENT + "> ?o3.\n" +
					"}";
			Explanation explain = connection
					.prepareTupleQuery(query)
					.explain(Explanation.Level.Executed);
			System.out.println(explain);

			try (TupleQueryResult evaluate = connection
					.prepareTupleQuery(query)
					.evaluate()) {
				List<BindingSet> collect = evaluate.stream().peek(a -> {
					System.out.println(a);
				}).collect(Collectors.toList());

				List<String> subjects = collect.stream()
						.map(b -> b.getValue("s"))
						.map(i -> (IRI) i)
						.map(IRI::getLocalName)
						.collect(Collectors.toList());

				Assertions.assertThat(subjects).isEqualTo(List.of("a", "b", "c", "d", "e"));
			}

			connection.commit();
		}
	}

	// Not implemented yet
//    @Test
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
