/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Håvard Ottestad
 */
public class ReduceNumberOfPlansTest {

	@Test
	public void testAddingTypeStatement()
			throws RDFParseException, UnsupportedRDFormatException, IOException, InterruptedException {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.init();
		Utils.loadShapeData(shaclSail, "reduceNumberOfPlansTest/shacl.trig");

		addDummyData(shaclSail);

		try (ShaclSailConnection connection = (ShaclSailConnection) shaclSail.getConnection()) {
			connection.begin();

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
			IRI person1 = Utils.Ex.createIri();
			connection.addStatement(person1, RDF.TYPE, Utils.Ex.Person);

			connection.prepareValidation();

			try (ConnectionsGroup connectionsGroup = connection.getConnectionsGroup()) {

				List<PlanNode> collect2 = shaclSail.getCachedShapes()
						.getDataAndRelease()
						.stream()
						.flatMap(s -> s.getShapes().stream())
						.map(shape -> shape.generatePlans(connectionsGroup, new ValidationSettings()))
						.filter(s -> !(s instanceof EmptyNode))
						.collect(Collectors.toList());
				Assertions.assertEquals(2, collect2.size());

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
	public void testRemovingPredicate()
			throws RDF4JException, UnsupportedRDFormatException, IOException, InterruptedException {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.init();
		Utils.loadShapeData(shaclSail, "reduceNumberOfPlansTest/shacl.trig");

		addDummyData(shaclSail);

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

			connection.prepareValidation();

			try (ConnectionsGroup connectionsGroup = connection.getConnectionsGroup()) {

				List<PlanNode> collect1 = shaclSail.getCachedShapes()
						.getDataAndRelease()
						.stream()
						.flatMap(s -> s.getShapes().stream())
						.map(shape -> shape.generatePlans(connectionsGroup, new ValidationSettings()))
						.filter(s -> !(s instanceof EmptyNode))
						.collect(Collectors.toList());
				Assertions.assertEquals(1, collect1.size());

			}

			connection.removeStatements(person1, Utils.Ex.ssn, vf.createLiteral("a"));

			connection.prepareValidation();

			try (ConnectionsGroup connectionsGroup = connection.getConnectionsGroup()) {

				List<PlanNode> collect2 = shaclSail.getCachedShapes()
						.getDataAndRelease()
						.stream()
						.flatMap(s -> s.getShapes().stream())
						.map(shape -> shape.generatePlans(connectionsGroup, new ValidationSettings()))
						.filter(s -> !(s instanceof EmptyNode))

						.collect(Collectors.toList());
				Assertions.assertEquals(1, collect2.size());
			}
			connection.removeStatements(person1, Utils.Ex.name, vf.createLiteral("c"));

			connection.prepareValidation();

			try (ConnectionsGroup connectionsGroup = connection.getConnectionsGroup()) {

				List<PlanNode> collect3 = shaclSail.getCachedShapes()
						.getDataAndRelease()
						.stream()
						.flatMap(s -> s.getShapes().stream())
						.map(shape -> shape.generatePlans(connectionsGroup, new ValidationSettings()))
						.filter(s -> !(s instanceof EmptyNode))

						.collect(Collectors.toList());
				Assertions.assertEquals(2, collect3.size());
			}
			connection.rollback();

		} finally {
			shaclSail.shutDown();
		}

	}

	private void addDummyData(ShaclSail shaclSail) {
		try (ShaclSailConnection connection = (ShaclSailConnection) shaclSail.getConnection()) {
			connection.begin();
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
			connection.commit();
		}
	}

}
