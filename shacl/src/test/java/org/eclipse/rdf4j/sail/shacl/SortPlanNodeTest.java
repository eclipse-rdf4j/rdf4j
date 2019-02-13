/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.mock.MockConsumePlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SortPlanNodeTest {

	@Test
	public void test(){

		SailRepository sailRepository = new SailRepository(new MemoryStore());
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			ValueFactory vf = connection.getValueFactory();
			connection.add(vf.createBNode("1"), RDF.TYPE, RDFS.RESOURCE);
			connection.add(vf.createBNode("2"), RDF.TYPE, RDFS.RESOURCE);
			connection.add(vf.createBNode("4"), RDF.TYPE, RDFS.RESOURCE);
			connection.add(vf.createBNode("3"), RDF.TYPE, RDFS.RESOURCE);
			connection.add(vf.createBNode("2"), RDF.TYPE, RDFS.RESOURCE);
			connection.add(vf.createBNode("2"), RDF.TYPE, RDFS.RESOURCE);
			connection.add(vf.createBNode("100"), RDF.TYPE, RDFS.RESOURCE);
			connection.add(vf.createBNode("99"), RDF.TYPE, RDFS.RESOURCE);
			connection.add(vf.createBNode("101"), RDF.TYPE, RDFS.RESOURCE);
			connection.add(vf.createBNode("98"), RDF.TYPE, RDFS.RESOURCE);
			connection.add(vf.createBNode("102"), RDF.TYPE, RDFS.RESOURCE);
		}

		Select select = new Select(sailRepository, "?a a rdfs:Resource");
		List<Tuple> sortedBySelect = new MockConsumePlanNode(select).asList();

		Sort sort = new Sort(new Select(sailRepository, "?a a rdfs:Resource"));
		List<Tuple> sortedBySort = new MockConsumePlanNode(sort).asList();


		assertEquals(sortedBySelect, sortedBySort);


	}

}
