/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Diagnostic replication of
 * {@link org.eclipse.rdf4j.testsuite.repository.RepositoryConnectionTest#testPreparedTupleQuery}.
 */
class LmdbQueryDebugTest {

	@ParameterizedTest
	@MethodSource("org.eclipse.rdf4j.testsuite.repository.RepositoryConnectionTest#parameters")
	void preparedTupleQuery(IsolationLevel level) throws Exception {
		File dataDir = java.nio.file.Files.createTempDirectory("lmdb-debug").toFile();
		Repository repo = new SailRepository(new LmdbStore(dataDir, new LmdbStoreConfig("spoc")));
		repo.init();
		try (RepositoryConnection con = repo.getConnection();
				RepositoryConnection con2 = repo.getConnection()) {
			con.setIsolationLevel(level);
			con2.setIsolationLevel(level);

			ValueFactory vf = repo.getValueFactory();
			IRI foafName = vf.createIRI("http://xmlns.com/foaf/0.1/name");
			IRI foafMbox = vf.createIRI("http://xmlns.com/foaf/0.1/mbox");
			IRI foafAgent = vf.createIRI("http://xmlns.com/foaf/0.1/Agent");

			IRI alice = vf.createIRI("urn:alice");
			IRI bob = vf.createIRI("urn:bob");
			IRI ctx1 = vf.createIRI("urn:ctx1");
			IRI ctx2 = vf.createIRI("urn:ctx2");

			con.begin(level);
			con.add(alice, foafName, vf.createLiteral("Alice"), ctx2);
			con.add(alice, foafMbox, vf.createLiteral("mailto:alice@example.org"), ctx2);
			con.add(ctx2, foafAgent, vf.createLiteral("Alice"));

			con.add(bob, foafName, vf.createLiteral("Bob"), ctx1);
			con.add(bob, foafMbox, vf.createLiteral("mailto:bob@example.org"), ctx1);
			con.add(ctx1, foafAgent, vf.createLiteral("Bob"));
			con.commit();

			long statements = Iterations.asList(con.getStatements(null, null, null, false)).size();
			long statements2 = Iterations.asList(con2.getStatements(null, null, null, false)).size();

			String queryBuilder = " PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n" +
					" SELECT ?name ?mbox \n" +
					" WHERE { [] foaf:name ?name; \n" +
					"            foaf:mbox ?mbox . }";
			TupleQuery query = con.prepareTupleQuery(queryBuilder);
			query.setBinding("name", vf.createLiteral("Bob"));

			try (TupleQueryResult result = query.evaluate()) {
				System.out.println("Isolation=" + level + " stmt=" + statements + " stmt2=" + statements2
						+ " result.size=" + Iterations.asList(result).size());
			}
		} finally {
			repo.shutDown();
			org.apache.commons.io.FileUtils.deleteDirectory(dataDir);
		}
	}
}
