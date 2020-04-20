/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;

import org.assertj.core.util.Files;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.Test;

public class NativeStoreTest {

	@Test
	public void testEmpty() throws IOException {

		File file = Files.newTemporaryFolder();

		SailRepository shaclSail = new SailRepository(new ShaclSail(new NativeStore(file)));
		shaclSail.init();

		shaclSail.shutDown();

		delete(file);
	}

	@Test(expected = ShaclSailValidationException.class)
	public void testPersistedShapes() throws Throwable {

		File file = Files.newTemporaryFolder();

		SailRepository shaclSail = new SailRepository(new ShaclSail(new NativeStore(file)));
		shaclSail.init();
		addShapes(shaclSail);
		shaclSail.shutDown();

		shaclSail = new SailRepository(new ShaclSail(new NativeStore(file)));
		shaclSail.init();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();

			StringReader invalidSampleData = new StringReader(String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .", "@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",

					"ex:peter a foaf:Person ;", "  foaf:age 20, \"30\"^^xsd:int  ."

			));

			connection.add(invalidSampleData, "", RDFFormat.TURTLE);
			try {
				connection.commit();
			} catch (RepositoryException exception) {
				throw exception.getCause();

			}
		}

		shaclSail.shutDown();

		delete(file);
	}

	private void addShapes(SailRepository shaclSail) throws IOException {
		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();

			StringReader shaclRules = new StringReader(String.join("\n", "", "@prefix ex: <http://example.com/ns#> .",
					"@prefix sh: <http://www.w3.org/ns/shacl#> .", "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

					"ex:PersonShape", "  a sh:NodeShape  ;", "  sh:targetClass foaf:Person ;",
					"  sh:property ex:PersonShapeProperty .",

					"ex:PersonShapeProperty ", "  sh:path foaf:age ;", "  sh:datatype xsd:int ;", "  sh:maxCount 1 ;",
					"  sh:minCount 1 ."));

			connection.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

		}
	}

	void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				delete(c);
			}
		}
		if (!f.delete()) {
			throw new FileNotFoundException("Failed to delete file: " + f);
		}
	}

}
