/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author jeen
 * @author Arjohn Kampman
 */
public abstract class RDFSchemaRepositoryConnectionTest extends RepositoryConnectionTest {

	public static IsolationLevel[] parameters() {
		return new IsolationLevel[] { IsolationLevels.NONE, IsolationLevels.READ_COMMITTED,
				IsolationLevels.SNAPSHOT_READ,
				IsolationLevels.SNAPSHOT, IsolationLevels.SERIALIZABLE };
	}

	private IRI woman;

	private IRI man;

	@Override
	protected void setupTest(IsolationLevel level) {
		super.setupTest(level);

		woman = vf.createIRI("http://example.org/Woman");
		man = vf.createIRI("http://example.org/Man");
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testDomainInference(IsolationLevel level) throws Exception {
		setupTest(level);

		testCon.begin();
		testCon.add(name, RDFS.DOMAIN, FOAF.PERSON);
		testCon.add(bob, name, nameBob);
		testCon.commit();

		assertTrue(testCon.hasStatement(bob, RDF.TYPE, FOAF.PERSON, true));
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testSubClassInference(IsolationLevel level) throws Exception {
		setupTest(level);

		testCon.begin();
		testCon.add(woman, RDFS.SUBCLASSOF, FOAF.PERSON);
		testCon.add(man, RDFS.SUBCLASSOF, FOAF.PERSON);
		testCon.add(alice, RDF.TYPE, woman);
		testCon.commit();

		assertTrue(testCon.hasStatement(alice, RDF.TYPE, FOAF.PERSON, true));
	}

	@ParameterizedTest
	@MethodSource("parameters")
	/**
	 * See https://github.com/eclipse/rdf4j/issues/1685
	 */
	public void testSubClassInferenceAfterRemoval(IsolationLevel level) throws Exception {
		setupTest(level);

		IRI mother = vf.createIRI("http://example.org/Mother");

		testCon.begin();
		testCon.add(FOAF.PERSON, RDFS.SUBCLASSOF, FOAF.AGENT);
		testCon.add(woman, RDFS.SUBCLASSOF, FOAF.PERSON);
		testCon.add(mother, RDFS.SUBCLASSOF, woman);
		testCon.commit();

		assertTrue(testCon.hasStatement(mother, RDFS.SUBCLASSOF, FOAF.AGENT, true));
		assertTrue(testCon.hasStatement(woman, RDFS.SUBCLASSOF, FOAF.AGENT, true));

		testCon.begin();
		testCon.remove(mother, RDFS.SUBCLASSOF, woman);
		testCon.commit();

		assertFalse(testCon.hasStatement(mother, RDFS.SUBCLASSOF, FOAF.AGENT, true));
		assertTrue(testCon.hasStatement(woman, RDFS.SUBCLASSOF, FOAF.AGENT, true));

	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testMakeExplicit(IsolationLevel level) throws Exception {
		setupTest(level);

		testCon.begin();
		testCon.add(woman, RDFS.SUBCLASSOF, FOAF.PERSON);
		testCon.add(alice, RDF.TYPE, woman);
		testCon.commit();

		assertTrue(testCon.hasStatement(alice, RDF.TYPE, FOAF.PERSON, true));

		testCon.begin();
		testCon.add(alice, RDF.TYPE, FOAF.PERSON);
		testCon.commit();

		assertTrue(testCon.hasStatement(alice, RDF.TYPE, FOAF.PERSON, true));
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testExplicitFlag(IsolationLevel level) throws Exception {
		setupTest(level);

		RepositoryResult<Statement> result = testCon.getStatements(RDF.TYPE, RDF.TYPE, null, true);
		try {
			assertTrue(result.hasNext(), "result should not be empty");
		} finally {
			result.close();
		}

		result = testCon.getStatements(RDF.TYPE, RDF.TYPE, null, false);
		try {
			assertFalse(result.hasNext(), "result should be empty");
		} finally {
			result.close();
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testInferencerUpdates(IsolationLevel level) throws Exception {
		setupTest(level);

		testCon.begin(IsolationLevels.READ_COMMITTED);

		testCon.add(bob, name, nameBob);
		testCon.remove(bob, name, nameBob);

		testCon.commit();

		assertFalse(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testInferencerQueryDuringTransaction(IsolationLevel level) throws Exception {
		setupTest(level);

		testCon.begin();

		testCon.add(bob, name, nameBob);
		assertTrue(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));

		testCon.commit();
	}

	@ParameterizedTest
	@EnumSource(IsolationLevels.class)
	public void testInferencerTransactionIsolation(IsolationLevel level) throws Exception {
		setupTest(level);

		if (IsolationLevels.NONE.isCompatibleWith(level)) {
			return;
		}
		testCon.begin();
		testCon.add(bob, name, nameBob);

		assertTrue(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
		assertFalse(testCon2.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));

		testCon.commit();

		assertTrue(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
		assertTrue(testCon2.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testContextStatementsNotDuplicated(IsolationLevel level) throws Exception {
		setupTest(level);

		testCon.add(bob, RDF.TYPE, FOAF.PERSON, RDF.FIRST);

		// TODO this test currently assumes that inferred triples are added to the null context. If we extend
		// the reasoner to support usage of other contexts, this will have to be amended.
		assertTrue(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true, (Resource) null),
				"inferred triple should have been added to null context");

// It used to expected behaviour that all inferred statements be added to the null context except those that already existed in some other context. There is no longer a check for if an inferred statement exists in other contexts.
//		assertFalse("input triple should not have been re-added as inferred",
//				testCon.hasStatement(bob, RDF.TYPE, FOAF.PERSON, true, (Resource) null));
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testContextStatementsNotDuplicated2(IsolationLevel level) throws Exception {
		setupTest(level);

		testCon.add(FOAF.PERSON, RDF.TYPE, RDFS.CLASS, RDF.FIRST);
		testCon.add(FOAF.PERSON, RDFS.SUBCLASSOF, FOAF.AGENT, RDF.FIRST);

		// TODO this test currently assumes that inferred triples are added to the null context. If we extend
		// the reasoner to support usage of other contexts, this will have to be amended.
		assertTrue(testCon.hasStatement(FOAF.AGENT, RDF.TYPE, RDFS.CLASS, true, (Resource) null),
				"inferred triple should have been added to null context");
		// It used to expected behaviour that all inferred statements be added to the null context except those that
		// already existed in some other context. There is no longer a check for if an inferred statement exists in
		// other contexts.
		// assertFalse("input triple should not have been re-added as inferred", testCon.hasStatement(FOAF.PERSON,
		// RDF.TYPE, RDFS.CLASS, true, (Resource) null));
		// assertFalse("input triple should not have been re-added as inferred", testCon.hasStatement(FOAF.PERSON,
		// RDFS.SUBCLASSOF, FOAF.AGENT, true, (Resource) null));
		assertTrue(testCon.hasStatement(FOAF.PERSON, RDFS.SUBCLASSOF, FOAF.AGENT, false),
				"input triple should be explicitly present");
		assertTrue(testCon.hasStatement(FOAF.PERSON, RDF.TYPE, RDFS.CLASS, false),
				"input triple should be explicitly present");

	}

}
