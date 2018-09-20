/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Håvard Ottestad
 */
public class ReduceNumberOfPlansTest {

	@Test
	public void testAddingTypeStatement() {
		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("reduceNumberOfPlansTest/shacl.ttl")));
		shaclSail.initialize();


		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();

			sailConnection.fillAddedAndRemovedStatementRepositories();
			List<PlanNode> collect = sailConnection.sail.nodeShapes.stream().flatMap(shape -> shape.generatePlans(sailConnection, shape).stream()).collect(Collectors.toList());

			assertEquals(0, collect.size());

			IRI person1 = Utils.Ex.createIri();
			connection.add(person1, RDF.TYPE, Utils.Ex.Person);
			sailConnection.fillAddedAndRemovedStatementRepositories();

			List<PlanNode> collect2 = sailConnection.sail.nodeShapes.stream().flatMap(shape -> shape.generatePlans(sailConnection, shape).stream()).collect(Collectors.toList());

			assertEquals(2, collect2.size());
			ValueFactory vf = connection.getValueFactory();
			connection.add(person1, Utils.Ex.ssn, vf.createLiteral("a"));
			connection.add(person1, Utils.Ex.ssn, vf.createLiteral("b"));
			connection.add(person1, Utils.Ex.name, vf.createLiteral("c"));


			connection.commit();


		}

	}

	@Test
	public void testRemovingPredicate() {
		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("reduceNumberOfPlansTest/shacl.ttl")));
		shaclSail.initialize();


		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();

			IRI person1 = Utils.Ex.createIri();

			ValueFactory vf = connection.getValueFactory();
			connection.add(person1, RDF.TYPE, Utils.Ex.Person);
			connection.add(person1, Utils.Ex.ssn, vf.createLiteral("a"));
			connection.add(person1, Utils.Ex.ssn, vf.createLiteral("b"));
			connection.add(person1, Utils.Ex.name, vf.createLiteral("c"));


			connection.commit();


			connection.begin();


			connection.remove(person1, Utils.Ex.ssn, vf.createLiteral("b"));

			sailConnection.fillAddedAndRemovedStatementRepositories();

			List<PlanNode> collect1 = sailConnection.sail.nodeShapes.stream().flatMap(shape -> shape.generatePlans(sailConnection, shape).stream()).collect(Collectors.toList());
			assertEquals(1, collect1.size());

			connection.remove(person1, Utils.Ex.ssn, vf.createLiteral("a"));


			sailConnection.fillAddedAndRemovedStatementRepositories();

			List<PlanNode> collect2 = sailConnection.sail.nodeShapes.stream().flatMap(shape -> shape.generatePlans(sailConnection, shape).stream()).collect(Collectors.toList());
			assertEquals(1, collect2.size());

			connection.remove(person1, Utils.Ex.name, vf.createLiteral("c"));
			sailConnection.fillAddedAndRemovedStatementRepositories();

			List<PlanNode> collect3 = sailConnection.sail.nodeShapes.stream().flatMap(shape -> shape.generatePlans(sailConnection, shape).stream()).collect(Collectors.toList());
			assertEquals(2, collect3.size());


			connection.rollback();


		}

	}

}
