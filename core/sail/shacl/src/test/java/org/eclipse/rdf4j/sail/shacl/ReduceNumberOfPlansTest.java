/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static junit.framework.TestCase.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.junit.Test;

/**
 * @author Håvard Ottestad
 */
public class ReduceNumberOfPlansTest {

	@Test
	public void testAddingTypeStatement() throws RDFParseException, UnsupportedRDFormatException, IOException {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.init();
		Utils.loadShapeData(shaclSail, "reduceNumberOfPlansTest/shacl.ttl");

		try (ShaclSailConnection connection = (ShaclSailConnection) shaclSail.getConnection()) {
			connection.begin();

			refreshAddedRemovedStatements(connection);
			try (ConnectionsGroup connectionsGroup = connection.getConnectionsGroup()) {

				List<PlanNode> collect = shaclSail.getCurrentShapes()
						.stream()
						.map(shape -> shape.generatePlans(connectionsGroup, false, false))
						.filter(s -> !(s instanceof EmptyNode))
						.collect(Collectors.toList());

				assertEquals(0, collect.size());
			}
			IRI person1 = Utils.Ex.createIri();
			connection.addStatement(person1, RDF.TYPE, Utils.Ex.Person);
			refreshAddedRemovedStatements(connection);
			try (ConnectionsGroup connectionsGroup = connection.getConnectionsGroup()) {

				List<PlanNode> collect2 = shaclSail.getCurrentShapes()
						.stream()
						.map(shape -> shape.generatePlans(connectionsGroup, false, false))
						.filter(s -> !(s instanceof EmptyNode))
						.collect(Collectors.toList());
				assertEquals(2, collect2.size());

			}
			ValueFactory vf = shaclSail.getValueFactory();
			connection.addStatement(person1, Utils.Ex.ssn, vf.createLiteral("a"));
			connection.addStatement(person1, Utils.Ex.ssn, vf.createLiteral("b"));
			connection.addStatement(person1, Utils.Ex.name, vf.createLiteral("c"));

			connection.commit();

		} finally {
			shaclSail.shutDown();
		}

	}

	@Test
	public void testRemovingPredicate() throws RDF4JException, UnsupportedRDFormatException, IOException {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.init();
		Utils.loadShapeData(shaclSail, "reduceNumberOfPlansTest/shacl.ttl");

		try (ShaclSailConnection connection = (ShaclSailConnection) shaclSail.getConnection()) {

			connection.begin();

			IRI person1 = Utils.Ex.createIri();

			ValueFactory vf = shaclSail.getValueFactory();
			connection.addStatement(person1, RDF.TYPE, Utils.Ex.Person);
			connection.addStatement(person1, Utils.Ex.ssn, vf.createLiteral("a"));
			connection.addStatement(person1, Utils.Ex.ssn, vf.createLiteral("b"));
			connection.addStatement(person1, Utils.Ex.name, vf.createLiteral("c"));
			connection.commit();

			connection.begin();

			connection.removeStatements(person1, Utils.Ex.ssn, vf.createLiteral("b"));

			refreshAddedRemovedStatements(connection);
			try (ConnectionsGroup connectionsGroup = connection.getConnectionsGroup()) {

				List<PlanNode> collect1 = shaclSail.getCurrentShapes()
						.stream()
						.map(shape -> shape.generatePlans(connectionsGroup, false, false))
						.filter(s -> !(s instanceof EmptyNode))
						.collect(Collectors.toList());
				assertEquals(1, collect1.size());

			}

			connection.removeStatements(person1, Utils.Ex.ssn, vf.createLiteral("a"));

			refreshAddedRemovedStatements(connection);
			try (ConnectionsGroup connectionsGroup = connection.getConnectionsGroup()) {

				List<PlanNode> collect2 = shaclSail.getCurrentShapes()
						.stream()
						.map(shape -> shape.generatePlans(connectionsGroup, false, false))
						.filter(s -> !(s instanceof EmptyNode))

						.collect(Collectors.toList());
				assertEquals(1, collect2.size());
			}
			connection.removeStatements(person1, Utils.Ex.name, vf.createLiteral("c"));
			refreshAddedRemovedStatements(connection);
			try (ConnectionsGroup connectionsGroup = connection.getConnectionsGroup()) {

				List<PlanNode> collect3 = shaclSail.getCurrentShapes()
						.stream()
						.map(shape -> shape.generatePlans(connectionsGroup, false, false))
						.filter(s -> !(s instanceof EmptyNode))

						.collect(Collectors.toList());
				assertEquals(2, collect3.size());
			}
			connection.rollback();

		} finally {
			shaclSail.shutDown();
		}

	}

	private void refreshAddedRemovedStatements(ShaclSailConnection connection) {

		connection.prepareValidation();

	}

}
