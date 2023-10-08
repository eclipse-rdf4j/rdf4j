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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Integration test suite for implementations of Repository.
 *
 * @author Jeen Broekstra
 */
@Timeout(value = 1, unit = TimeUnit.MINUTES)
public abstract class RepositoryTest {

	@BeforeAll
	public static void setUpClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterAll
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	private static final String MBOX = "mbox";

	private static final String NAME = "name";

	protected static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";

	public static final String TEST_DIR_PREFIX = "/testcases/";

	protected Repository testRepository;

	protected ValueFactory vf;

	protected Resource bob;

	protected Resource alice;

	protected Resource alexander;

	protected IRI name;

	protected IRI mbox;

	protected final IRI publisher = DC.PUBLISHER;

	protected IRI unknownContext;

	protected IRI context1;

	protected IRI context2;

	protected Literal nameAlice;

	protected Literal nameBob;

	protected Literal mboxAlice;

	protected Literal mboxBob;

	protected Literal Александър;

	@BeforeEach
	public void setUp() throws Exception {
		testRepository = createRepository();

		vf = testRepository.getValueFactory();

		// Initialize values
		bob = vf.createBNode();
		alice = vf.createBNode();

		name = vf.createIRI(FOAF_NS + NAME);
		mbox = vf.createIRI(FOAF_NS + MBOX);

		nameAlice = vf.createLiteral("Alice");
		nameBob = vf.createLiteral("Bob");

		mboxAlice = vf.createLiteral("alice@example.org");
		mboxBob = vf.createLiteral("bob@example.org");

	}

	@AfterEach
	public void tearDown() {
		testRepository.shutDown();
	}

	/**
	 * Gets an (uninitialized) instance of the repository that should be tested.
	 *
	 * @return an uninitialized repository.
	 */
	protected abstract Repository createRepository();

	@Test
	public void testShutdownFollowedByInit() {
		testRepository.init();
		RepositoryConnection conn = testRepository.getConnection();
		try {
			conn.add(bob, mbox, mboxBob);
			assertTrue(conn.hasStatement(bob, mbox, mboxBob, true));
		} finally {
			conn.close();
		}

		testRepository.shutDown();
		testRepository.init();

		conn = testRepository.getConnection();
		try {
			conn.add(bob, mbox, mboxBob);
			assertTrue(conn.hasStatement(bob, mbox, mboxBob, true));
		} finally {
			conn.close();
		}
	}

	@Test
	public void testAutoInit() {
		try (RepositoryConnection conn = testRepository.getConnection()) {
			conn.add(bob, mbox, mboxBob);
			assertTrue(conn.hasStatement(bob, mbox, mboxBob, true));
			assertTrue(testRepository.isInitialized());
		}
	}

}
