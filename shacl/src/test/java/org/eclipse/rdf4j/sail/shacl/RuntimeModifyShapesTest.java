/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

import java.io.IOException;

public class RuntimeModifyShapesTest {

	@Test(expected = NoShapesLoadedException.class)
	public void checkForExceptionWhenValidatingWithoutShapes() {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}


	}

	@Test()
	public void checkForNoExceptionWithEmptyTransaction() {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.commit();
		}


	}

	@Test(expected = IllegalStateException.class)
	public void checkForExceptionWhenModifyingShapes() throws IOException {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(RuntimeModifyShapesTest.class.getClassLoader().getResourceAsStream("shaclDatatype.ttl"), "http://example.com/", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();
			connection.begin();
			connection.add(RuntimeModifyShapesTest.class.getClassLoader().getResourceAsStream("shacl.ttl"), "http://example.com/", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();
		}


	}

}
