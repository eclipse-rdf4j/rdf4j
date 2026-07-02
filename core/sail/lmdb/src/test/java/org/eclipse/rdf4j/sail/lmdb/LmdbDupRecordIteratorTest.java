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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Focused integration tests that exercise {@link LmdbDupRecordIterator} via the public Sail API. The scenarios reflect
 * the assumptions made in the iterator about prefix binding and duplicate handling.
 */
class LmdbDupRecordIteratorTest {

	@TempDir
	File dataDir;

	private LmdbStore store;
	private NotifyingSailConnection con;
	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@BeforeEach
	void setUp() throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc");
		config.setDupsortIndices(true);
		config.setDupsortRead(true);
		store = new LmdbStore(dataDir, config);
		store.init();
		con = store.getConnection();
	}

	@AfterEach
	void tearDown() throws Exception {
		if (con != null) {
			con.close();
			con = null;
		}
		if (store != null) {
			store.shutDown();
			store = null;
		}
	}

	@Test
	void predicateAndObjectBoundReturnsAllSubjects() throws Exception {
		Resource ctx = vf.createIRI("urn:ctx");
		loadTypeStatements(ctx);

		int count = countStatements(null, RDF.TYPE, RDFS.CLASS, true);
		assertEquals(3, count, "Expected duplicate iterator to surface all matching subjects");
	}

	@Test
	void predicateObjectAndContextBoundRestrictsToContext() throws Exception {
		Resource ctx1 = vf.createIRI("urn:ctx:1");
		Resource ctx2 = vf.createIRI("urn:ctx:2");
		loadTypeStatements(ctx1);
		con.begin();
		con.addStatement(vf.createIRI("urn:other"), RDF.TYPE, RDFS.CLASS, ctx2);
		con.commit();

		int countCtx1 = countStatements(null, RDF.TYPE, RDFS.CLASS, true, ctx1);
		int countCtx2 = countStatements(null, RDF.TYPE, RDFS.CLASS, true, ctx2);

		assertEquals(2, countCtx1, "Context-specific lookup should include only ctx1 statements");
		assertEquals(1, countCtx2, "Context-specific lookup should include only ctx2 statements");
	}

	@Test
	void singleLeadingBindingFallsBackGracefully() throws Exception {
		loadTypeStatements(vf.createIRI("urn:ctx:fallback"));
		int count = countStatements(null, RDF.TYPE, null, true);
		assertEquals(3, count, "Fallback path should still return all type statements");
	}

	@Test
	void duplicateIterationDisabledFallsBackToStandardIterator() throws Exception {
		tearDown();

		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc");
		config.setDupsortIndices(true);
		config.setDupsortRead(false);
		store = new LmdbStore(dataDir, config);
		store.init();
		con = store.getConnection();

		loadTypeStatements(vf.createIRI("urn:ctx:disabled"));

		int count = countStatements(null, RDF.TYPE, RDFS.CLASS, true);
		assertEquals(3, count, "Store should still return complete results when dupsort is disabled");
	}

	@Test
	void includeInferredStatementsWhenRequested() throws Exception {
		Resource ctx = vf.createIRI("urn:ctx:inf");
		loadTypeStatements(ctx);

		con.begin();
		((LmdbStoreConnection) con).addInferredStatement(vf.createIRI("urn:dan"), RDF.TYPE, RDFS.CLASS, ctx);
		con.commit();

		int explicitOnly = countStatements(null, RDF.TYPE, RDFS.CLASS, false);
		int withInferred = countStatements(null, RDF.TYPE, RDFS.CLASS, true);

		assertEquals(3, explicitOnly, "Explicit view should ignore inferred additions");
		assertEquals(4, withInferred, "Requested inferred statements should be surfaced");
	}

	@Test
	void rangePositioningSkipsOverSmallerPrefixes() throws Exception {
		Resource ctx = vf.createIRI("urn:ctx:range");
		IRI otherType = vf.createIRI("urn:type:other");
		IRI targetType = vf.createIRI("urn:type:target");

		con.begin();
		con.addStatement(vf.createIRI("urn:noise"), RDF.TYPE, otherType, ctx);
		con.addStatement(vf.createIRI("urn:alpha"), RDF.TYPE, targetType, ctx);
		con.addStatement(vf.createIRI("urn:beta"), RDF.TYPE, targetType, ctx);
		con.addStatement(vf.createIRI("urn:gamma"), RDF.TYPE, targetType, ctx);
		con.commit();

		int count = countStatements(null, RDF.TYPE, targetType, true, ctx);
		assertEquals(3, count, "Iterator should position on the first matching predicate/object prefix");
	}

	private void loadTypeStatements(Resource ctx) throws Exception {
		con.begin();
		con.addStatement(vf.createIRI("urn:alice"), RDF.TYPE, RDFS.CLASS);
		con.addStatement(vf.createIRI("urn:bob"), RDF.TYPE, RDFS.CLASS, ctx);
		con.addStatement(vf.createIRI("urn:carol"), RDF.TYPE, RDFS.CLASS, ctx);
		con.commit();
	}

	private int countStatements(Resource subj, IRI pred, org.eclipse.rdf4j.model.Value obj, boolean includeInferred,
			Resource... ctx) throws Exception {
		int count = 0;
		try (var iter = con.getStatements(subj, pred, obj, includeInferred, ctx)) {
			while (iter.hasNext()) {
				iter.next();
				count++;
			}
		}
		return count;
	}

	@Test
	@Timeout(5)
	public void test() {

		// Add some data to the repository
		con.begin();
		con.addStatement(Values.bnode(), RDF.TYPE, RDFS.CLASS);
		con.addStatement(Values.bnode(), RDF.TYPE, RDFS.CLASS);
		con.addStatement(Values.bnode(), RDF.TYPE, Values.bnode(), Values.bnode());
		con.addStatement(Values.bnode(), RDF.TYPE, Values.bnode(), Values.bnode());
		con.commit();

		try (CloseableIteration<? extends Statement> statements = con.getStatements(null, RDF.TYPE, RDFS.CLASS, true)) {
			int count = 0;
			int max = 100;
			while (max-- > 0 && statements.hasNext()) {
				count++;
				Statement next = statements.next();
				System.out.println(next);
			}
			assertThat(count).isEqualTo(2);
		}

	}

	@Test
	void clearRemovesAllStatements() throws Exception {
		con.begin();
		for (int i = 0; i < 5; i++) {
			con.addStatement(vf.createIRI("urn:s" + i), RDF.TYPE, RDFS.CLASS, vf.createIRI("urn:ctx" + (i % 2)));
		}
		con.commit();

		con.begin();
		con.clear();
		con.commit();

		assertEquals(0, countStatements(null, null, null, true));
	}

	@Test
	void clearAfterSnapshotScenario() throws Exception {
		// mimic SailIsolationLevelTest.setUp sequence
		con.begin();
		con.addStatement(RDF.NIL, RDF.TYPE, RDF.LIST);
		con.commit();

		con.begin();
		con.clear();
		con.commit();

		assertEquals(0, countStatements(null, null, null, true));
	}

}
