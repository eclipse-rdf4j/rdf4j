/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.config.ProxyRepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for {@link LocalRepositoryManager}
 * 
 * @author Jeen Broekstra
 */
public class LocalRepositoryManagerTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private LocalRepositoryManager manager;

	private File datadir;

	private static final String TEST_REPO = "test";

	private static final String PROXY_ID = "proxy";

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		datadir = tempDir.newFolder("local-repositorymanager-test");
		manager = new LocalRepositoryManager(datadir);
		manager.initialize();

		// Create configurations for the SAIL stack, and the repository
		// implementation.
		manager.addRepositoryConfig(
				new RepositoryConfig(TEST_REPO, new SailRepositoryConfig(new MemoryStoreConfig(true))));

		// Create configuration for proxy repository to previous repository.
		manager.addRepositoryConfig(new RepositoryConfig(PROXY_ID, new ProxyRepositoryConfig(TEST_REPO)));
	}

	/**
	 * @throws IOException if a problem occurs deleting temporary resources
	 */
	@After
	public void tearDown() throws IOException {
		manager.shutDown();
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.repository.manager.LocalRepositoryManager#getRepository(java.lang.String)} .
	 *
	 * @throws RepositoryException       if a problem occurs accessing the repository
	 * @throws RepositoryConfigException if a problem occurs accessing the repository
	 */
	@Test
	public void testGetRepository() throws RepositoryConfigException, RepositoryException {
		Repository rep = manager.getRepository(TEST_REPO);
		assertNotNull("Expected repository to exist.", rep);
		assertTrue("Expected repository to be initialized.", rep.isInitialized());
		rep.shutDown();
		rep = manager.getRepository(TEST_REPO);
		assertNotNull("Expected repository to exist.", rep);
		assertTrue("Expected repository to be initialized.", rep.isInitialized());
	}

	@Test
	public void testRestartManagerWithoutTransaction() throws Exception {
		Repository rep = manager.getRepository(TEST_REPO);
		assertNotNull("Expected repository to exist.", rep);
		assertTrue("Expected repository to be initialized.", rep.isInitialized());
		try (RepositoryConnection conn = rep.getConnection()) {
			conn.add(conn.getValueFactory().createIRI("urn:sesame:test:subject"), RDF.TYPE, OWL.ONTOLOGY);
			assertEquals(1, conn.size());
		} finally {
			rep.shutDown();
			manager.shutDown();
		}

		manager = new LocalRepositoryManager(datadir);
		manager.initialize();
		Repository rep2 = manager.getRepository(TEST_REPO);
		assertNotNull("Expected repository to exist.", rep2);
		assertTrue("Expected repository to be initialized.", rep2.isInitialized());
		try (RepositoryConnection conn2 = rep2.getConnection()) {
			assertEquals(1, conn2.size());
		} finally {
			rep2.shutDown();
			manager.shutDown();
		}

	}

	@Test
	public void testRestartManagerWithTransaction() throws Exception {
		Repository rep = manager.getRepository(TEST_REPO);
		assertNotNull("Expected repository to exist.", rep);
		assertTrue("Expected repository to be initialized.", rep.isInitialized());
		try (RepositoryConnection conn = rep.getConnection()) {
			conn.begin();
			conn.add(conn.getValueFactory().createIRI("urn:sesame:test:subject"), RDF.TYPE, OWL.ONTOLOGY);
			conn.commit();
			assertEquals(1, conn.size());
		} finally {
			rep.shutDown();
			manager.shutDown();
		}

		manager = new LocalRepositoryManager(datadir);
		manager.initialize();
		Repository rep2 = manager.getRepository(TEST_REPO);
		assertNotNull("Expected repository to exist.", rep2);
		assertTrue("Expected repository to be initialized.", rep2.isInitialized());
		try (RepositoryConnection conn2 = rep2.getConnection()) {
			assertEquals(1, conn2.size());
		} finally {
			rep2.shutDown();
			manager.shutDown();
		}

	}

	/**
	 * Test method for {@link RepositoryManager.isSafeToRemove(String)}.
	 *
	 * @throws RepositoryException       if a problem occurs during execution
	 * @throws RepositoryConfigException if a problem occurs during execution
	 */
	@Test
	public void testIsSafeToRemove() throws RepositoryException, RepositoryConfigException {
		assertThat(manager.isSafeToRemove(PROXY_ID)).isTrue();
		assertThat(manager.isSafeToRemove(TEST_REPO)).isFalse();
		manager.removeRepository(PROXY_ID);
		assertThat(manager.hasRepositoryConfig(PROXY_ID)).isFalse();
		;
		assertThat(manager.isSafeToRemove(TEST_REPO)).isTrue();
		;
	}

	@Test
	@Deprecated
	public void testAddToSystemRepository() {
		RepositoryConfig config = manager.getRepositoryConfig(TEST_REPO);
		manager.addRepositoryConfig(new RepositoryConfig(SystemRepository.ID, new SystemRepositoryConfig()));
		manager.shutDown();
		manager = new LocalRepositoryManager(datadir);
		manager.initialize();
		try (RepositoryConnection con = manager.getSystemRepository().getConnection()) {
			Model model = new TreeModel();
			config.setID("changed");
			config.export(model, con.getValueFactory().createBNode());
			con.begin();
			con.add(model, con.getValueFactory().createBNode());
			con.commit();
		}
		assertTrue(manager.hasRepositoryConfig("changed"));
	}

	@Test
	@Deprecated
	public void testModifySystemRepository() {
		RepositoryConfig config = manager.getRepositoryConfig(TEST_REPO);
		manager.addRepositoryConfig(new RepositoryConfig(SystemRepository.ID, new SystemRepositoryConfig()));
		manager.shutDown();
		manager = new LocalRepositoryManager(datadir);
		manager.initialize();
		try (RepositoryConnection con = manager.getSystemRepository().getConnection()) {
			Model model = new TreeModel();
			config.setTitle("Changed");
			config.export(model, con.getValueFactory().createBNode());
			Resource ctx = RepositoryConfigUtil.getContext(con, config.getID());
			con.begin();
			con.clear(ctx);
			con.add(model, ctx == null ? con.getValueFactory().createBNode() : ctx);
			con.commit();
		}
		assertEquals("Changed", manager.getRepositoryConfig(TEST_REPO).getTitle());
	}

	@Test
	@Deprecated
	public void testRemoveFromSystemRepository() {
		RepositoryConfig config = manager.getRepositoryConfig(TEST_REPO);
		manager.addRepositoryConfig(new RepositoryConfig(SystemRepository.ID, new SystemRepositoryConfig()));
		manager.shutDown();
		manager = new LocalRepositoryManager(datadir);
		manager.initialize();
		try (RepositoryConnection con = manager.getSystemRepository().getConnection()) {
			Model model = new TreeModel();
			config.setID("changed");
			config.export(model, con.getValueFactory().createBNode());
			con.begin();
			con.add(model, con.getValueFactory().createBNode());
			con.commit();
		}
		assertTrue(manager.hasRepositoryConfig("changed"));
		try (RepositoryConnection con = manager.getSystemRepository().getConnection()) {
			con.begin();
			con.clear(RepositoryConfigUtil.getContext(con, config.getID()));
			con.commit();
		}
		assertFalse(manager.hasRepositoryConfig(config.getID()));
	}

	/**
	 * Regression test for adding new repositories when legacy SYSTEM repository is still present See also GitHub issue
	 * 1077
	 */
	@Test
	public void testAddWithExistingSysRepository() {
		new File(datadir, "repositories/SYSTEM").mkdir();
		try {
			RepositoryImplConfig cfg = new SailRepositoryConfig(new MemoryStoreConfig());
			manager.addRepositoryConfig(new RepositoryConfig("test-01", cfg));
			manager.addRepositoryConfig(new RepositoryConfig("test-02", cfg));
		} catch (RepositoryConfigException e) {
			fail(e.getMessage());
		}
	}
}
