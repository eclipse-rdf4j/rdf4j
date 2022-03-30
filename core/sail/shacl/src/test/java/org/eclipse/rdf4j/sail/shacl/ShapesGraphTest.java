/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ShapesGraphTest {

	static final private String EX = "http://example.com/ns#";

	static final private IRI data1 = Values.iri(EX, "data1");
	static final private IRI data2 = Values.iri(EX, "data2");

	static final private IRI Human = Values.iri(EX, "Human");

	static final private IRI laura = Values.iri(EX, "laura");
	static final private IRI steve = Values.iri(EX, "steve");
	static final private IRI olivia = Values.iri(EX, "olivia");
	static final private IRI charlie = Values.iri(EX, "charlie");

	@Test
	public void testValidSplitAcrossGraphs() throws Throwable {

		test(repository -> {

			try (RepositoryConnection connection = repository.getConnection()) {
				connection.begin();
				connection.add(laura, RDF.TYPE, FOAF.PERSON, data1);
				connection.add(laura, FOAF.NAME, Values.literal("Laura"), data1);

				connection.add(laura, RDF.TYPE, FOAF.PERSON, data2);
				connection.add(laura, FOAF.NAME, Values.literal("Laura"), data2);

				connection.add(laura, FOAF.KNOWS, steve, data1);
				connection.add(steve, RDF.TYPE, FOAF.PERSON, data1);
				connection.add(steve, FOAF.NAME, Values.literal("Steve"), data1);

				connection.add(laura, FOAF.KNOWS, olivia, data2);
				connection.add(olivia, RDF.TYPE, Human, data2);

				connection.commit();
			}

		});

	}

	@Test
	public void testInvalid() throws Throwable {

		test(repository -> {

			assertThrows(RepositoryException.class, () -> {
				try (RepositoryConnection connection = repository.getConnection()) {
					connection.begin();
					connection.add(laura, RDF.TYPE, FOAF.PERSON, data2);
					connection.commit();
				}
			});

		});

	}

	@Test
	public void testInvalidUnionGraph() throws Throwable {

		test(repository -> {

			try (RepositoryConnection connection = repository.getConnection()) {
				connection.begin();
				connection.add(laura, RDF.TYPE, FOAF.PERSON, data2);
				connection.add(laura, FOAF.NAME, Values.literal("Laura"), data2);
				connection.commit();
			}

			assertThrows(RepositoryException.class, () -> {
				try (RepositoryConnection connection = repository.getConnection()) {
					connection.begin();

					connection.add(laura, FOAF.PHONE, Values.literal(1));
					connection.add(laura, FOAF.PHONE, Values.literal(1), data1);
					connection.add(laura, FOAF.PHONE, Values.literal(1), data2);

					connection.commit();
				}
			});

		});

	}

	@Test
	public void testInvalidSplitAcrossGraphs() throws Throwable {

		test(repository -> {

			assertThrows(RepositoryException.class, () -> {
				try (RepositoryConnection connection = repository.getConnection()) {
					connection.begin();
					connection.add(laura, RDF.TYPE, FOAF.PERSON);
					connection.add(laura, FOAF.NAME, Values.literal("Laura"), data2);
					connection.commit();
				}
			});

		});

	}

	@Test
	public void testInvalidSwitchGraph() throws Throwable {

		test(repository -> {

			assertThrows(RepositoryException.class, () -> {
				try (RepositoryConnection connection = repository.getConnection()) {
					connection.begin();
					connection.add(laura, RDF.TYPE, FOAF.PERSON);
					connection.add(laura, FOAF.NAME, Values.literal("Laura"));
					connection.commit();

					connection.begin();
					connection.remove(laura, FOAF.NAME, null);
					connection.add(laura, FOAF.NAME, Values.literal("Laura"), data2);
					connection.commit();

				}
			});

		});

	}

	@Test
	public void testValidationRequired() throws IOException, InterruptedException {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		SailRepository repository = new SailRepository(shaclSail);

		shaclSail.setShapesGraphs(Set.of(
				Values.iri(EX, "peopleKnowPeopleShapes"),
				Values.iri(EX, "peopleKnowHumansShapes")
		));

		loadShapes(repository);

		try (ShaclSailConnection connection = (ShaclSailConnection) shaclSail.getConnection()) {
			connection.begin();
			connection.addStatement(Values.bnode(), RDF.TYPE, FOAF.PERSON, data1);

			connection.prepareValidation();

			try (ConnectionsGroup connectionsGroup = connection.getConnectionsGroup()) {

				List<PlanNode> collect = shaclSail.getCachedShapes()
						.getDataAndRelease()
						.stream()
						.flatMap(s -> s.getShapes().stream())
						.map(shape -> shape.generatePlans(connectionsGroup, new ValidationSettings()))
						.filter(s -> !(s instanceof EmptyNode))
						.collect(Collectors.toList());

				Assertions.assertEquals(0, collect.size());
			}
		}

		repository.shutDown();

	}

	@Test
	public void testDefaultShapesGraph() throws IOException {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		SailRepository repository = new SailRepository(shaclSail);

		loadShapes(repository);

		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.add(laura, RDF.TYPE, FOAF.PERSON, Values.iri("http://example.org/differentGraph"));
			connection.add(laura, FOAF.PHONE, Values.literal(12345678));
			connection.add(laura, FOAF.PHONE, Values.literal(12345678), data2);
			connection.commit();
		}

		assertThrows(RepositoryException.class, () -> {
			try (RepositoryConnection connection = repository.getConnection()) {
				connection.begin();
				connection.add(laura, FOAF.PHONE, Values.literal(12345678), data1);
				connection.commit();
			}
		});

		repository.shutDown();

	}

	private void loadShapes(SailRepository repository) throws IOException {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(ShapesGraphTest.class.getClassLoader().getResource("multipleShapesGraphs.trig"));
			connection.commit();
		}
	}

	private void test(Consumer<Repository> testCase) throws Throwable {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		SailRepository repository = new SailRepository(shaclSail);

		shaclSail.setShapesGraphs(Set.of(
				RDF4J.SHACL_SHAPE_GRAPH,
				Values.iri(EX, "peopleKnowPeopleShapes"),
				Values.iri(EX, "peopleKnowHumansShapes"),
				Values.iri(EX, "mustHaveNameShapes"),
				Values.iri(EX, "maxFiveAcquaintances"),
				Values.iri(EX, "nestedKnowsShouldHaveAge")
		));

		loadShapes(repository);

		try {
			testCase.accept(repository);
		} catch (RepositoryException e) {
			ShaclSailValidationReportHelper.printValidationReport(e.getCause(), System.err);
			throw e;
		}

		shaclSail.shutDown();

	}

}
