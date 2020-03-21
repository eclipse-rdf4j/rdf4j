/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStoreImplForTests;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencerConnection;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class InferenceTest {

	// TODO: These tests have been removed on the develop branch. Figure out why, and what do to with them now!

	@Test
	@Ignore
	public void testSail() {

		ExtensibleStoreImplForTests extensibleStoreImplForTests = new ExtensibleStoreImplForTests();
		SchemaCachingRDFSInferencer schemaCachingRDFSInferencer = new SchemaCachingRDFSInferencer(
				extensibleStoreImplForTests);

		try (SchemaCachingRDFSInferencerConnection connection = schemaCachingRDFSInferencer.getConnection()) {
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			BNode bNode = vf.createBNode();
			connection.begin();
			connection.addStatement(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
			connection.begin();
			List<Statement> statements = Iterations
					.asList(connection.getStatements(bNode, RDF.TYPE, RDFS.RESOURCE, true));
			connection.commit();

			statements.forEach(System.out::println);
			assertEquals(1, statements.size());

		}

	}

	@Test
	public void testRepo() {

		ExtensibleStoreImplForTests extensibleStoreImplForTests = new ExtensibleStoreImplForTests();
		SailRepository schemaCachingRDFSInferencer = new SailRepository(
				new SchemaCachingRDFSInferencer(extensibleStoreImplForTests));

		try (RepositoryConnection connection = schemaCachingRDFSInferencer.getConnection()) {
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			BNode bNode = vf.createBNode();
			connection.begin();
			connection.add(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
			connection.begin();
			List<Statement> statements = Iterations
					.asList(connection.getStatements(bNode, RDF.TYPE, RDFS.RESOURCE, true));
			connection.commit();

			statements.forEach(System.out::println);
			assertEquals(1, statements.size());

		}

	}

}
