/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PrepareCommitTest {

	@Test
	public void testFailureWhenChangesAfterPrepare() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shacl.trig");

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			// due to optimizations in the ShaclSail, changes after prepare has run will only be detected if there is
			// data in the base sail already!
			connection.begin();
			connection.addStatement(RDFS.RESOURCE, RDFS.LABEL, SimpleValueFactory.getInstance().createLiteral("label"));
			connection.commit();

			connection.begin();
			connection.addStatement(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			connection.prepare();
			assertThrows(IllegalStateException.class, () -> {
				try {
					connection.removeStatements(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);

				} catch (RepositoryException e) {
					throw e.getCause();
				}
			});

			connection.commit();

		} finally {
			shaclSail.shutDown();
		}

	}

	@Test
	public void testPrepareFollowedByRollback() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shaclMinCountZero.trig");
		NotifyingSailConnection conn = shaclSail.getConnection();
		try {
			Model otherShaclData = Rio.parse(getClass().getResourceAsStream("/shacl.trig"), "",
					RDFFormat.TRIG);
			conn.begin();
			conn.clear(RDF4J.SHACL_SHAPE_GRAPH);
			otherShaclData.forEach(st -> conn.addStatement(st.getSubject(), st.getPredicate(), st.getObject(),
					RDF4J.SHACL_SHAPE_GRAPH));
			IRI bob = SimpleValueFactory.getInstance().createIRI("http://example.org/bob");
			conn.addStatement(bob, RDF.TYPE, RDFS.RESOURCE);

			conn.prepare(); // should fail because bob has no label
			Assertions.fail("constraint violation not detected on prepare call");
		} catch (ShaclSailValidationException e) {
			conn.rollback();

			// check that original shacl data (shaclMinCountZero) has been restored
			Model restoredShapeGraph = QueryResults
					.asModel(conn.getStatements(null, null, null, true, RDF4J.SHACL_SHAPE_GRAPH));

			Set<Literal> minCountValues = Models
					.objectLiterals(restoredShapeGraph.getStatements(null, SHACL.MIN_COUNT, null));
			assertThat(minCountValues).hasSize(1).allMatch(l -> l.intValue() == 0);
		} finally {
			conn.close();
			shaclSail.shutDown();
		}
	}

	@Test
	public void testMultiplePrepare() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shacl.trig");

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			connection.begin();
			connection.addStatement(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			connection.prepare();
			connection.commit();

			connection.begin();
			connection.removeStatements(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			connection.prepare();
			connection.commit();

		}

		shaclSail.shutDown();
	}

	@Test
	public void testWithoutPrepare() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shacl.trig");

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			connection.begin();
			connection.addStatement(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			connection.commit();

		}

		shaclSail.shutDown();
	}

	@Test
	public void testPrepareAfterRollback() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shacl.trig");

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			connection.begin();
			connection.addStatement(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			connection.prepare();
			connection.prepare();
			connection.rollback();
			connection.rollback();

			connection.begin();
			connection.addStatement(RDFS.SUBCLASSOF, RDFS.SUBPROPERTYOF, RDFS.SUBCLASSOF);
			connection.prepare();
			connection.commit();

		}

		shaclSail.shutDown();
	}

	@Test
	public void testAutomaticRollback() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shacl.trig");

		BNode bNode = SimpleValueFactory.getInstance().createBNode();

		NotifyingSailConnection connection = null;
		try {
			connection = shaclSail.getConnection();
			connection.begin();
			connection.addStatement(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.prepare();
			connection.commit();

		} catch (ShaclSailValidationException ignored) {
		} finally {
			if (connection != null) {
				// check that nothing has been rolled back yet
				Assertions.assertTrue(connection.hasStatement(bNode, RDF.TYPE, RDFS.RESOURCE, false));
				connection.close();
			}
		}

		// check that close() called rollback
		try (NotifyingSailConnection connection1 = shaclSail.getConnection()) {
			Assertions.assertFalse(connection1.hasStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE, false));
		}

		shaclSail.shutDown();
	}

	@Test
	public void testAutomaticRollback2() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shacl.trig");

		boolean exception = false;
		BNode bNode = SimpleValueFactory.getInstance().createBNode();

		NotifyingSailConnection connection = null;
		try {
			connection = shaclSail.getConnection();
			connection.begin();
			connection.addStatement(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

		} catch (ShaclSailValidationException ignored) {
			exception = true;
		} finally {
			if (connection != null) {
				// check that nothing has been rolled back yet
				Assertions.assertTrue(connection.hasStatement(bNode, RDF.TYPE, RDFS.RESOURCE, false));
				connection.close();
			}
		}

		// check that close() called rollback
		try (NotifyingSailConnection connection1 = shaclSail.getConnection()) {
			Assertions.assertFalse(connection1.hasStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE, false));
		}

		shaclSail.shutDown();

		Assertions.assertTrue(exception);
	}

	@Test
	public void testAutomaticRollbackRepository() throws IOException {
		SailRepository shaclSail = Utils.getInitializedShaclRepository("shacl.trig");

		boolean exception = false;
		BNode bNode = SimpleValueFactory.getInstance().createBNode();

		SailRepositoryConnection connection = null;
		try {
			connection = shaclSail.getConnection();
			connection.begin();
			connection.add(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

		} catch (RepositoryException ignored) {
			exception = true;
		} finally {
			if (connection != null) {
				// check that nothing has been rolled back yet
				Assertions.assertTrue(connection.hasStatement(bNode, RDF.TYPE, RDFS.RESOURCE, false));
				connection.close();
			}
		}

		// check that close() called rollback
		try (SailRepositoryConnection connection1 = shaclSail.getConnection()) {
			Assertions.assertFalse(connection1.hasStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE, false));
		}

		shaclSail.shutDown();

		Assertions.assertTrue(exception);
	}

}
