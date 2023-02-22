/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * An extension of RDFStoreTest for testing the class {@link NativeStore}.
 */
public class NativeSailStoreTest {

	@TempDir
	File tempFolder;

	protected Repository repo;

	protected final ValueFactory F = SimpleValueFactory.getInstance();

	protected final IRI CTX_1 = F.createIRI("urn:one");
	protected final IRI CTX_2 = F.createIRI("urn:two");
	protected final IRI CTX_INV = F.createIRI("urn:invalid");

	protected final Statement S0 = F.createStatement(F.createIRI("http://example.org/0"), RDFS.LABEL,
			F.createLiteral("zero"));
	protected final Statement S1 = F.createStatement(F.createIRI("http://example.org/1"), RDFS.LABEL,
			F.createLiteral("one"));
	protected final Statement S2 = F.createStatement(F.createIRI("http://example.org/2"), RDFS.LABEL,
			F.createLiteral("two"));

	@BeforeEach
	public void before() throws Exception {
		File dataDir = new File(tempFolder, "dbmodel");
		dataDir.mkdir();
		repo = new SailRepository(new NativeStore(dataDir, "spoc,posc"));
		repo.init();

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(S0);
			conn.add(S1, CTX_1);
			conn.add(S2, CTX_2);
		}
	}

	@Test
	public void testRemoveValidContext() {
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.remove((IRI) null, null, null, CTX_1);
		}
		try (RepositoryConnection conn = repo.getConnection()) {
			assertTrue(conn.hasStatement(S0, false), "Statement 0 incorrectly removed");
			assertFalse(conn.hasStatement(S1, false, CTX_1), "Statement 1 still not removed");
			assertTrue(conn.hasStatement(S2, false, CTX_2), "Statement 2 incorrectly removed");
		}
	}

	@Test
	public void testRemoveEmptyContext() {
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.remove((IRI) null, null, null, (Resource) null);
		}
		try (RepositoryConnection conn = repo.getConnection()) {
			assertFalse(conn.hasStatement(S0, false), "Statement 0 still not removed");
			assertTrue(conn.hasStatement(S1, false, CTX_1), "Statement 1 incorrectly removed");
			assertTrue(conn.hasStatement(S2, false, CTX_2), "Statement 2 incorrectly removed");
		}
	}

	@Test
	public void testRemoveInvalidContext() {
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.remove((IRI) null, null, null, CTX_INV);
		}
		try (RepositoryConnection conn = repo.getConnection()) {
			assertTrue(conn.hasStatement(S0, false), "Statement 0 incorrectly removed");
			assertTrue(conn.hasStatement(S1, false, CTX_1), "Statement 1 incorrectly removed");
			assertTrue(conn.hasStatement(S2, false, CTX_2), "Statement 2 incorrectly removed");
		}
	}

	@Test
	public void testRemoveMultipleValidContext() {
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.remove((IRI) null, null, null, CTX_1, CTX_2);
		}
		try (RepositoryConnection conn = repo.getConnection()) {
			assertTrue(conn.hasStatement(S0, false), "Statement 0 incorrectly removed");
			assertFalse(conn.hasStatement(S1, false, CTX_1), "Statement 1 still not removed");
			assertFalse(conn.hasStatement(S2, false, CTX_2), "Statement 2 still not removed");
		}
	}

	@Test
	public void testClearMultipleValidContext() {
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.clear(CTX_1, CTX_2);
		}
		try (RepositoryConnection conn = repo.getConnection()) {
			assertTrue(conn.hasStatement(S0, false), "Statement 0 incorrectly removed");
			assertFalse(conn.hasStatement(S1, false, CTX_1), "Statement 1 still not removed");
			assertFalse(conn.hasStatement(S2, false, CTX_2), "Statement 2 still not removed");
		}
	}

	@AfterEach
	public void after() throws Exception {
		repo.shutDown();
	}
}
