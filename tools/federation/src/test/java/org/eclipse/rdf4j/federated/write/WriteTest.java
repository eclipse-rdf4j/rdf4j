/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.write;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.SPARQLBaseTest;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointBase;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WriteTest extends SPARQLBaseTest {

	@BeforeEach
	public void nativeStoreOnly() {
		assumeNativeStore();
	}

	@Test
	public void testSimpleWrite() throws Exception {
		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Iterator<Endpoint> iter = federationContext().getEndpointManager().getAvailableEndpoints().iterator();
		EndpointBase ep1 = (EndpointBase) iter.next();
		ep1.setWritable(true);
		Endpoint ep2 = iter.next();

		List<Statement> stmts = null;
		Statement st = simpleStatement();

		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {
			conn.add(st);

			// test that statement is returned from federation
			stmts = Iterations.asList(conn.getStatements(null, null, null, true));
			Assertions.assertEquals(1, stmts.size());
			Assertions.assertEquals(st, stmts.get(0));
		}

		// check that the statement is actually written to endpoint 1
		try (RepositoryConnection ep1Conn = ep1.getConnection()) {
			stmts = Iterations.asList(ep1Conn.getStatements(null, null, null, true));
			Assertions.assertEquals(1, stmts.size());
			Assertions.assertEquals(st, stmts.get(0));
		}

		// check that endpoint 2 is empty
		try (RepositoryConnection ep2Conn = ep2.getConnection()) {
			stmts = Iterations.asList(ep2Conn.getStatements(null, null, null, true));
			Assertions.assertEquals(0, stmts.size());
		}
	}

	@Test
	public void testReadOnlyFederation() throws Exception {

		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Assertions.assertEquals(false, fedxRule.getRepository().isWritable());

		Assertions.assertThrows(UnsupportedOperationException.class, () -> {
			Statement st = simpleStatement();
			try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {
				try {
					conn.add(st);
				} catch (RuntimeException e) {
					// rollback to avoid a stack trace in the output
					conn.rollback();
					throw e;
				}
			}
		});

	}

	@Test
	public void testSimpleUpdateQuery() throws Exception {

		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Iterator<Endpoint> iter = federationContext().getEndpointManager().getAvailableEndpoints().iterator();
		EndpointBase ep1 = (EndpointBase) iter.next();
		ep1.setWritable(true);

		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {
			Update update = conn.prepareUpdate(QueryLanguage.SPARQL,
					"PREFIX : <http://example.org/> INSERT { :subject a :Person } WHERE { }");
			update.execute();

			// test that statement is returned from federation
			List<Statement> stmts = Iterations.asList(conn.getStatements(null, null, null, true));
			Assertions.assertEquals(1, stmts.size());
			Assertions.assertEquals(RDF.TYPE, stmts.get(0).getPredicate());
		}
	}

	@Test
	public void testSimpleUpdateQuery_insertData() throws Exception {

		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Iterator<Endpoint> iter = federationContext().getEndpointManager().getAvailableEndpoints().iterator();
		EndpointBase ep1 = (EndpointBase) iter.next();
		ep1.setWritable(true);

		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {
			Update update = conn.prepareUpdate(QueryLanguage.SPARQL,
					"PREFIX ex: <http://example.org/> INSERT DATA { ex:subject a ex:Person } ");
			update.execute();

			// test that statement is returned from federation
			List<Statement> stmts = Iterations.asList(conn.getStatements(null, null, null, true));
			Assertions.assertEquals(1, stmts.size());
			Assertions.assertEquals(RDF.TYPE, stmts.get(0).getPredicate());
		}
	}

	@Test
	public void testSimpleRemove() throws Exception {
		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Iterator<Endpoint> iter = federationContext().getEndpointManager().getAvailableEndpoints().iterator();
		EndpointBase ep1 = (EndpointBase) iter.next();
		ep1.setWritable(true);

		Statement st = simpleStatement();

		try (RepositoryConnection ep1Conn = ep1.getRepository().getConnection()) {
			ep1Conn.add(st);
		}

		// test that statement is returned from federation
		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {

			List<Statement> stmts = Iterations.asList(conn.getStatements(null, null, null, true));
			Assertions.assertEquals(1, stmts.size());
			Assertions.assertEquals(st, stmts.get(0));

			conn.remove(st.getSubject(), null, null);

			Assertions.assertEquals(0, conn.size());
		}
	}

	protected Statement simpleStatement() {
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI subject = vf.createIRI("http://example.org/person1");
		return vf.createStatement(subject, RDF.TYPE, FOAF.PERSON);
	}
}
