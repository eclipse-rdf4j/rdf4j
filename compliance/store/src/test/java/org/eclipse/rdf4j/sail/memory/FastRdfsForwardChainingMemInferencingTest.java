/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.InferencingTest;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingSchemaCachingRDFSInferencer;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class FastRdfsForwardChainingMemInferencingTest extends InferencingTest {

	@Override
	protected Sail createSail() {
		Sail sailStack = new ForwardChainingSchemaCachingRDFSInferencer(new MemoryStore(), true);
		return sailStack;
	}

	@Test
	public void testBlankNodePredicateInference(){
		Repository sailRepository = new SailRepository(createSail());
		sailRepository.initialize();
		ValueFactory vf = sailRepository.getValueFactory();

		try (RepositoryConnection connection = sailRepository.getConnection()) {
			BNode bNode = vf.createBNode();
			connection.add(vf.createStatement(vf.createIRI("http://a"), RDFS.SUBPROPERTYOF, bNode)); // 1
			connection.add(vf.createStatement(bNode, RDFS.DOMAIN, vf.createIRI("http://c"))); // 2
			connection.add(vf.createStatement(vf.createIRI("http://d"), vf.createIRI("http://a"), vf.createIRI("http://e"))); // 3
		}

		try (RepositoryConnection connection = sailRepository.getConnection()) {
			boolean correctInference = connection.hasStatement(vf.createIRI("http://d"), RDF.TYPE, vf.createIRI("http://c"), true);
			assertTrue("d should be type c, because 3 and 1 entail 'd _:bNode e' with 2 entail 'd type c'", correctInference);
		}

	}
}
