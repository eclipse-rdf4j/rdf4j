/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.repository.util.RDFLoader;
import org.eclipse.rdf4j.repository.util.RepositoryUtil;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for checking Native Store index consistency.
 *
 * @author Jeen Broekstra
 */
public class NativeStoreConsistencyTest {
	private static final Logger logger = LoggerFactory.getLogger(NativeStoreConsistencyTest.class);

	/*-----------*
	 * Variables *
	 *-----------*/

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	/*---------*
	 * Methods *
	 *---------*/

	@Test
	public void testSES1867IndexCorruption() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI oldContext = vf.createIRI("http://example.org/oldContext");
		IRI newContext = vf.createIRI("http://example.org/newContext");

		File dataDir = tempDir.newFolder("nativestore-consistency");

		Repository repo = new SailRepository(new NativeStore(dataDir, "spoc,psoc"));
		repo.initialize();

		RepositoryConnection conn = repo.getConnection();

		// Step1: setup the initial database state
		logger.info("Preserving initial state ...");
		conn.begin();
		RDFInserter inserter = new RDFInserter(conn) {
			private int count;

			@Override
			protected void addStatement(Resource subj, IRI pred, Value obj, Resource ctxt) {
				super.addStatement(subj, pred, obj, ctxt);
				if (count++ % 1000 == 0) {
					con.commit();
					con.begin();
				}
			}
		};
		RDFLoader loader = new RDFLoader(conn.getParserConfig(), conn.getValueFactory());
		loader.load(getClass().getResourceAsStream("/nativestore-testdata/SES-1867/initialState.nq"), "",
				RDFFormat.NQUADS, inserter);
		conn.commit();
		logger.info("Number of statements: " + conn.size());

		// Step 2: in a single transaction remove "oldContext", then add
		// statements to "newContext"
		conn.begin();

		logger.info("Removing old context");
		conn.remove((Resource) null, (IRI) null, (Value) null, oldContext);

		logger.info("Adding updated context");
		conn.add(getClass().getResourceAsStream("/nativestore-testdata/SES-1867/newTriples.nt"), "", RDFFormat.NTRIPLES,
				newContext);
		conn.commit();

		// Step 3: check whether oldContext is actually empty
		List<Statement> stmts = Iterations.asList(conn.getStatements(null, null, null, false, oldContext));
		logger.info("Not deleted statements: " + stmts.size());

		conn.close();
		repo.shutDown();

		// Step 4: check the repository size with SPOC only
		new File(dataDir, "triples.prop").delete(); // delete triples.prop to
		// update index usage
		repo = new SailRepository(new NativeStore(dataDir, "spoc"));
		repo.initialize();
		conn = repo.getConnection();
		logger.info("Repository size with SPOC index only: " + conn.size());
		Model spocStatements = Iterations.addAll(conn.getStatements(null, null, null, false), new LinkedHashModel());
		conn.close();
		repo.shutDown();

		// Step 5: check the repository size with PSOC only
		new File(dataDir, "triples.prop").delete(); // delete triples.prop to
		// update index usage
		repo = new SailRepository(new NativeStore(dataDir, "psoc"));
		repo.initialize();
		conn = repo.getConnection();
		logger.info("Repository size with PSOC index only: " + conn.size());
		Model psocStatements = Iterations.addAll(conn.getStatements(null, null, null, false), new LinkedHashModel());
		conn.close();
		repo.shutDown();

		// Step 6: computing the differences of the contents of the indices
		logger.info("Computing differences of sets...");

		Collection<? extends Statement> differenceA = RepositoryUtil.difference(spocStatements, psocStatements);
		Collection<? extends Statement> differenceB = RepositoryUtil.difference(psocStatements, spocStatements);

		logger.info("Difference SPOC MINUS PSOC: " + differenceA.size());
		logger.info("Difference PSOC MINUS SPOC: " + differenceB.size());

		logger.info("Different statements in SPOC MINUS PSOC (Mind the contexts):");
		for (Statement st : differenceA) {
			logger.error("  * " + st);
		}

		logger.info("Different statements in PSOC MINUS SPOC (Mind the contexts):");
		for (Statement st : differenceB) {
			logger.error("  * " + st);
		}

		assertEquals(0, differenceA.size());
		assertEquals(0, differenceB.size());
	}

}
