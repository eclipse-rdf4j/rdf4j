/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation;

import static org.junit.Assert.assertFalse;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

public class FederationSparqlTest {

	@Test
	public void test181Issue() throws Exception {
		SailRepository repo1 = new SailRepository(new MemoryStore());
		SailRepository repo2 = new SailRepository(new MemoryStore());
		Federation fed = new Federation();
		fed.addMember(repo1);
		fed.addMember(repo2);
		SailRepository repoFed = new SailRepository(fed);
		repoFed.initialize();

		try (RepositoryConnection conn = repo1.getConnection()) {
			conn.add(getClass().getResource("/testcases-sparql-1.0-w3c/data-r2/algebra/var-scope-join-1.ttl"),
					conn.getValueFactory().createIRI("http://example/g1"));
		}
		try (RepositoryConnection conn = repo2.getConnection()) {
			conn.add(getClass().getResource("/testcases-sparql-1.0-w3c/data-r2/algebra/var-scope-join-1.ttl"),
					conn.getValueFactory().createIRI("http://example/g2"));
		}

		String query = "PREFIX : <http://example/> SELECT * { graph :g1 {?X  :name 'paul'} { graph :g2 {?Y :name 'george' . OPTIONAL { ?X :email ?Z } } } }";
		boolean hasResults;
		try (RepositoryConnection conn = repoFed.getConnection()) {
			TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
			TupleQueryResult tqr = tq.evaluate();
			hasResults = tqr.hasNext();
		}
		assertFalse(hasResults);
	}
}
