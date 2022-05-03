/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation;
import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.PerformanceHint.SerialValidation;

public class CloseThreadsAndReleaseResourcesTest {

	@Test
	public void test() throws IOException {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setParallelValidation(true);
		SailRepository sailRepository = new SailRepository(shaclSail);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin(SerialValidation);
			connection.add(Values.bnode(), Values.iri("http://example.org/fjkejfoiwjf"), Values.bnode());
			connection.add(new StringReader(""), RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();
		}

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin(ParallelValidation);
			connection.add(Values.bnode(), Values.iri("http://example.org/fjkejfoiwjf"), Values.bnode());
			connection.commit();
		}


	}

}
