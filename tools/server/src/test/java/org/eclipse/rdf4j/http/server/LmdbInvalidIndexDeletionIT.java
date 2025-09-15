/**
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.rdf4j.http.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Jetty-based integration test that reproduces a user report: Creating an LMDB repository with an invalid triple index
 * (e.g., "cposc") then attempting to delete it via HTTP fails.
 *
 * Expected behavior: either reject creation upfront or allow deletion. This test asserts deletion succeeds; it
 * currently fails, exposing the bug.
 */
public class LmdbInvalidIndexDeletionIT {

	private static TestServer server;

	@BeforeAll
	public static void startServer() throws Exception {
		server = new TestServer();
		try {
			server.start();
		} catch (Exception e) {
			server.stop();
			throw e;
		}
	}

	@AfterAll
	public static void stopServer() throws Exception {
		server.stop();
	}

	@Test
	void deletionSucceedsAfterInvalidLmdbInit() throws Exception {
		String id = "badlmdb-server";

		// Build a minimal LMDB Sail config without depending on LMDB classes
		// by exporting with the LMDB sail type and the tripleIndexes property.
		class GenericLmdbConfig extends AbstractSailImplConfig {
			private final String tripleIndexes;

			GenericLmdbConfig(String type, String tripleIndexes) {
				super(type);
				this.tripleIndexes = tripleIndexes;
			}

			@Override
			public org.eclipse.rdf4j.model.Resource export(org.eclipse.rdf4j.model.Model m) {
				org.eclipse.rdf4j.model.Resource node = super.export(m);
				ValueFactory vf = SimpleValueFactory.getInstance();
				IRI tripleIdx = vf.createIRI("http://rdf4j.org/config/sail/lmdb#tripleIndexes");
				m.add(node, tripleIdx, vf.createLiteral(tripleIndexes));
				return node;
			}
		}

		GenericLmdbConfig lmdbConfig = new GenericLmdbConfig("rdf4j:LmdbStore", "cposc");
		RepositoryConfig repoConfig = new RepositoryConfig(id, new SailRepositoryConfig(lmdbConfig));

		RemoteRepositoryManager manager = RemoteRepositoryManager.getInstance(TestServer.SERVER_URL);
		try {
			// Create config on server (does not initialize the underlying store yet)
			manager.addRepositoryConfig(repoConfig);

			// Trigger initialization by opening a connection; expected to fail due to invalid index
			Repository httpRepo = new org.eclipse.rdf4j.repository.http.HTTPRepository(
					Protocol.getRepositoryLocation(TestServer.SERVER_URL, id));
			try (RepositoryConnection conn = httpRepo.getConnection()) {
				// attempt a trivial call to ensure init
				conn.size();
			} catch (RepositoryException expected) {
				// initialization fails as LMDB rejects invalid index spec
			}

			// Now attempt to delete the repository; expected to succeed
			boolean removed = manager.removeRepository(id);
			assertThat(removed).isTrue();
		} finally {
			// best-effort cleanup if assertion failed
			try {
				manager.removeRepository(id);
			} catch (Exception ignore) {
			}
			manager.shutDown();
		}
	}
}
