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
package org.eclipse.rdf4j.repository.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Configurations;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
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
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for {@link LocalRepositoryManager}
 *
 * @author Jeen Broekstra
 */
public class LocalRepositoryManagerIntegrationTest extends RepositoryManagerIntegrationTest {

	@TempDir
	File datadir;

	private static final String TEST_REPO = "test";

	private static final String PROXY_ID = "proxy";

	/**
	 */
	@BeforeEach
	@Override
	public void setUp() {
		subject = new LocalRepositoryManager(datadir);
		subject.init();

		// Create configurations for the SAIL stack, and the repository
		// implementation.
		subject.addRepositoryConfig(
				new RepositoryConfig(TEST_REPO, new SailRepositoryConfig(new MemoryStoreConfig(true))));

		// Create configuration for proxy repository to previous repository.
		subject.addRepositoryConfig(new RepositoryConfig(PROXY_ID, new ProxyRepositoryConfig(TEST_REPO)));
	}

	/**
	 */
	@AfterEach
	public void tearDown() {
		subject.shutDown();
	}

	/**
	 * Test method for {@link LocalRepositoryManager#getRepository(java.lang.String)} .
	 *
	 * @throws RepositoryException       if a problem occurs accessing the repository
	 * @throws RepositoryConfigException if a problem occurs accessing the repository
	 */
	@Test
	public void testGetRepository() throws RepositoryConfigException, RepositoryException {
		Repository rep = subject.getRepository(TEST_REPO);
		assertThat(rep).isNotNull();
		assertThat(rep.isInitialized()).isTrue();
		rep.shutDown();
		rep = subject.getRepository(TEST_REPO);
		assertThat(rep).isNotNull();
		assertThat(rep.isInitialized()).isTrue();
	}

	@Test
	public void testRestartManagerWithoutTransaction() {
		Repository rep = subject.getRepository(TEST_REPO);
		assertThat(rep).isNotNull();
		assertThat(rep.isInitialized()).isTrue();
		try (RepositoryConnection conn = rep.getConnection()) {
			conn.add(conn.getValueFactory().createIRI("urn:sesame:test:subject"), RDF.TYPE, OWL.ONTOLOGY);
			assertThat(conn.size()).isEqualTo(1);
		} finally {
			rep.shutDown();
			subject.shutDown();
		}

		subject = new LocalRepositoryManager(datadir);
		subject.init();
		Repository rep2 = subject.getRepository(TEST_REPO);
		assertThat(rep2).isNotNull();
		assertThat(rep2.isInitialized()).isTrue();
		try (RepositoryConnection conn2 = rep2.getConnection()) {
			assertThat(conn2.size()).isEqualTo(1);
		} finally {
			rep2.shutDown();
			subject.shutDown();
		}

	}

	@Test
	public void testRestartManagerWithTransaction() {
		Repository rep = subject.getRepository(TEST_REPO);
		assertThat(rep).isNotNull();
		assertThat(rep.isInitialized()).isTrue();
		try (RepositoryConnection conn = rep.getConnection()) {
			conn.begin();
			conn.add(conn.getValueFactory().createIRI("urn:sesame:test:subject"), RDF.TYPE, OWL.ONTOLOGY);
			conn.commit();
			assertThat(conn.size()).isEqualTo(1);
		} finally {
			rep.shutDown();
			subject.shutDown();
		}

		subject = new LocalRepositoryManager(datadir);
		subject.init();
		Repository rep2 = subject.getRepository(TEST_REPO);
		assertThat(rep2).isNotNull();
		assertThat(rep2.isInitialized()).isTrue();
		try (RepositoryConnection conn2 = rep2.getConnection()) {
			assertThat(conn2.size()).isEqualTo(1);
		} finally {
			rep2.shutDown();
			subject.shutDown();
		}

	}

	/**
	 * Test method for {@link RepositoryManager#isSafeToRemove(String)}.
	 *
	 * @throws RepositoryException       if a problem occurs during execution
	 * @throws RepositoryConfigException if a problem occurs during execution
	 */
	@Test
	public void testIsSafeToRemove() throws RepositoryException, RepositoryConfigException {
		assertThat(subject.isSafeToRemove(PROXY_ID)).isTrue();
		assertThat(subject.isSafeToRemove(TEST_REPO)).isFalse();
		subject.removeRepository(PROXY_ID);
		assertThat(subject.hasRepositoryConfig(PROXY_ID)).isFalse();
		assertThat(subject.isSafeToRemove(TEST_REPO)).isTrue();
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
			subject.addRepositoryConfig(new RepositoryConfig("test-01", cfg));
			subject.addRepositoryConfig(new RepositoryConfig("test-02", cfg));
		} catch (RepositoryConfigException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetRepositoryConfig_Legacy() throws Exception {
		// set up legacy configuration
		new File(datadir, "repositories/legacy").mkdir();
		InputStream in = getClass().getResourceAsStream("/fixtures/memory-legacy.ttl");
		Model model = Rio.parse(in, RDFFormat.TURTLE);
		Rio.write(model, new FileOutputStream(new File(datadir, "repositories/legacy/config.ttl")),
				RDFFormat.TURTLE);

		RepositoryConfig config = subject.getRepositoryConfig("legacy");
		assertThat(config).isNotNull();
		assertThat(config.getTitle()).isEqualTo("Legacy Test Repository");

		// verify manager has converted the config file.
		File convertedConfig = new File(datadir, "repositories/legacy/config.ttl");

		Model convertedModel = Rio.parse(new FileInputStream(convertedConfig), convertedConfig.toURI().toString(),
				RDFFormat.TURTLE);
		assertThat(convertedModel.getNamespaces()).contains(CONFIG.NS);
		assertThat(Configurations.hasLegacyConfiguration(convertedModel)).isFalse();
	}

	@Test
	public void testGetRepositoryConfig_Legacy_useLegacy() throws Exception {
		// set up legacy configuration
		new File(datadir, "repositories/legacy").mkdir();
		InputStream in = getClass().getResourceAsStream("/fixtures/memory-legacy.ttl");
		Model model = Rio.parse(in, RDFFormat.TURTLE);
		Rio.write(model, new FileOutputStream(new File(datadir, "repositories/legacy/config.ttl")),
				RDFFormat.TURTLE);

		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "true");
		RepositoryConfig config = subject.getRepositoryConfig("legacy");
		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "");

		assertThat(config).isNotNull();
		assertThat(config.getTitle()).isEqualTo("Legacy Test Repository");

		// verify manager has NOT converted the config file.
		File convertedConfig = new File(datadir, "repositories/legacy/config.ttl");

		Model convertedModel = Rio.parse(new FileInputStream(convertedConfig), convertedConfig.toURI().toString(),
				RDFFormat.TURTLE);
		assertThat(convertedModel.getNamespaces()).doesNotContain(CONFIG.NS);
		assertThat(Configurations.hasLegacyConfiguration(convertedModel)).isTrue();
	}

	@Test
	public void testAddConfig_validation() throws Exception {
		InputStream in = getClass().getResourceAsStream("/fixtures/memory-invalid.ttl");
		Model model = Rio.parse(in, RDFFormat.TURTLE);

		assertThatExceptionOfType(RepositoryConfigException.class)
				.isThrownBy(() -> RepositoryConfigUtil.getRepositoryConfig(model, "Test"));
	}

}
