/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

public class InferredContextTest {

	SimpleValueFactory vf = SimpleValueFactory.getInstance();

	BNode bNode = vf.createBNode();
	BNode type = vf.createBNode();
	BNode context = vf.createBNode();

	@Test
	public void testInferrecContextNull() {
		SchemaCachingRDFSInferencer sail = new SchemaCachingRDFSInferencer(new MemoryStore());
		sail.setAddInferredStatementsToDefaultContext(true);

		try (SchemaCachingRDFSInferencerConnection connection = sail.getConnection()) {
			connection.begin();
			connection.addStatement(bNode, RDF.TYPE, type, context);
			connection.commit();

			assertFalse(connection.hasStatement(bNode, RDF.TYPE, RDFS.RESOURCE, true, context));
			assertTrue(connection.hasStatement(bNode, RDF.TYPE, RDFS.RESOURCE, true));

		}

	}

	@Test
	public void testInferrecContextNoNull() {
		SchemaCachingRDFSInferencer sail = new SchemaCachingRDFSInferencer(new MemoryStore());
		sail.setAddInferredStatementsToDefaultContext(false);

		try (SchemaCachingRDFSInferencerConnection connection = sail.getConnection()) {
			connection.begin();
			connection.addStatement(bNode, RDF.TYPE, type, context);
			connection.commit();

			assertTrue(connection.hasStatement(bNode, RDF.TYPE, RDFS.RESOURCE, true, context));

			try (CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(bNode,
					RDF.TYPE, RDFS.RESOURCE, true)) {
				while (statements.hasNext()) {
					Statement next = statements.next();
					assertEquals("Context should be equal", context, next.getContext());
				}
			}
		}

	}

	@Test
	public void testDefaultBehaviour() {
		SchemaCachingRDFSInferencer sail = new SchemaCachingRDFSInferencer(new MemoryStore());

		assertFalse("Current default behaviour should be to add all statements to default context",
				sail.isAddInferredStatementsToDefaultContext());

	}

}
