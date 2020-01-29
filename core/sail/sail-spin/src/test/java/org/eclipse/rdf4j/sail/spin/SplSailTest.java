/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SPL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.DedupingInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Runs the spl test cases.
 */
public class SplSailTest {

	private Repository repo;

	private RepositoryConnection conn;

	@Before
	public void setup() throws RepositoryException {
		NotifyingSail baseSail = new MemoryStore();
		DedupingInferencer deduper = new DedupingInferencer(baseSail);
		NotifyingSail rdfsInferencer = new SchemaCachingRDFSInferencer(deduper, false);
		SpinSail spinSail = new SpinSail(rdfsInferencer);
		repo = new SailRepository(spinSail);
		repo.initialize();
		conn = repo.getConnection();
	}

	@After
	public void tearDown() throws RepositoryException {
		if (conn != null) {
			conn.close();
		}
		if (repo != null) {
			repo.shutDown();
		}
	}

	@Test
	public void runTests() throws Exception {
		ValueFactory vf = conn.getValueFactory();
		loadRDF("/schema/owl.ttl");
		conn.add(vf.createStatement(vf.createURI("test:run"), RDF.TYPE, vf.createURI(SPL.NAMESPACE, "RunTestCases")));
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, "prefix spin: <http://spinrdf.org/spin#> "
				+ "prefix spl: <http://spinrdf.org/spl#> "
				+ "select ?testCase ?expected ?actual where {(<test:run>) spin:select (?testCase ?expected ?actual)}");
		try ( // returns failed tests
				TupleQueryResult tqr = tq.evaluate()) {
			while (tqr.hasNext()) {
				BindingSet bs = tqr.next();
				Value testCase = bs.getValue("testCase");
				Value expected = bs.getValue("expected");
				Value actual = bs.getValue("actual");
				assertEquals(testCase.stringValue(), expected, actual);
			}
		}
	}

	private void loadRDF(String path) throws IOException, RDFParseException, RepositoryException {
		URL url = getClass().getResource(path);
		try (InputStream in = url.openStream()) {
			conn.add(in, url.toString(), RDFFormat.TURTLE);
		}
	}
}
