/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * An extension of RDFStoreTest for testing the class {@link LmdbStore}.
 */
public class LmdbGroupByTest {

	/*-----------*
	 * Variables *
	 *-----------*/

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();
	private File dataDir;
	private LmdbStore sail;

	/*---------*
	 * Methods *
	 *---------*/

	@Before
	public void setup() throws SailException {
		try {
			dataDir = tempDir.newFolder();
			sail = new LmdbStore(dataDir, new LmdbStoreConfig("spoc,posc"));
			sail.init();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	@After
	public void shutDown() throws SailException {
		sail.shutDown();
	}

	@Test
	public void testGroupBy() throws Exception {
		try (NotifyingSailConnection con = sail.getConnection()) {
			con.begin();
			con.addStatement(RDF.TYPE, RDF.TYPE, RDF.TYPE, RDF.ALT);
			con.addStatement(RDF.TYPE, RDF.TYPE, RDF.TYPE, RDF.BAG);
			con.commit();
		}
		try (NotifyingSailConnection con = sail.getConnection()) {
			ParsedQuery parseQuery = new SPARQLParser()
					.parseQuery("SELECT ?s (SAMPLE(?o) AS ?os) WHERE {?s ?p ?o} GROUP BY ?s", RDF.NAMESPACE);
			try (CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate = con
					.evaluate(parseQuery.getTupleExpr(), null, EmptyBindingSet.getInstance(), false)) {
				assertTrue(evaluate.hasNext());
				assertNotNull(evaluate.next());
				assertFalse(evaluate.hasNext());
			}
		}
	}

}
