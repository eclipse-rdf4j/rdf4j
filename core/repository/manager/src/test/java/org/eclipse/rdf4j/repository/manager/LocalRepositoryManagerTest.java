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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;

import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link LocalRepositoryManager}.
 * <p>
 * Note a lot of the functionality for the local manager can only be tested by integrating with other RDF4J modules -
 * more comprehensive tests are therefore located in LocalRepositoryManagerIntegationTest in the
 * rdfj4-repository-compliance module.
 *
 * @author Jeen Broekstra
 */
public class LocalRepositoryManagerTest extends RepositoryManagerTest {

	/**
	 */
	@BeforeEach
	public void setUp(@TempDir File datadir) {
		subject = new LocalRepositoryManager(datadir);
		subject.init();
	}

	/**
	 */
	@AfterEach
	public void tearDown() {
		subject.shutDown();
	}

	@Test
	public void testAddRepositoryConfig_validation() {
		RepositoryConfig config = mock(RepositoryConfig.class);
		doThrow(RepositoryConfigException.class).when(config).validate();

		assertThrows(RepositoryConfigException.class, () -> subject.addRepositoryConfig(config));
	}

	@Test
	public void testGetRepositoryDir_rejectsIDsOutsideRepositoriesDir(@TempDir File datadir) {
		LocalRepositoryManager manager = new LocalRepositoryManager(datadir);

		File repositoriesDir = manager.resolvePath(LocalRepositoryManager.REPOSITORIES_DIR);
		assertThat(manager.getRepositoryDir("test")).isEqualTo(new File(repositoriesDir, "test"));

		assertThrows(RepositoryConfigException.class, () -> manager.getRepositoryDir("../test"));
		assertThrows(RepositoryConfigException.class, () -> manager.getRepositoryDir("test/../../test"));
		assertThrows(RepositoryConfigException.class, () -> manager.getRepositoryDir("test/nested"));
		assertThrows(RepositoryConfigException.class, () -> manager.getRepositoryDir(".."));
		assertThrows(RepositoryConfigException.class, () -> manager.getRepositoryDir("."));
		assertThrows(RepositoryConfigException.class, () -> manager.getRepositoryDir(""));
	}

	@Test
	public void testAddRepositoryConfig_rejectsIDOutsideRepositoriesDir(@TempDir File datadir) throws Exception {
		LocalRepositoryManager manager = new LocalRepositoryManager(new File(datadir, "base"));
		manager.init();
		try {
			RepositoryConfig config = mock(RepositoryConfig.class);
			when(config.getID()).thenReturn("../../escaped");

			assertThrows(RepositoryConfigException.class, () -> manager.addRepositoryConfig(config));
			assertThat(new File(datadir, "escaped")).doesNotExist();
		} finally {
			manager.shutDown();
		}
	}

	@Test
	public void testRemoveRepository_rejectsIDOutsideRepositoriesDir(@TempDir File datadir) throws Exception {
		LocalRepositoryManager manager = new LocalRepositoryManager(new File(datadir, "base"));
		manager.init();
		try {
			File victim = new File(datadir, "victim");
			Files.createDirectories(victim.toPath());
			Files.writeString(new File(victim, "keep.txt").toPath(), "keep");

			assertThrows(RepositoryConfigException.class, () -> manager.removeRepository("../../victim"));
			assertThat(new File(victim, "keep.txt")).exists();
		} finally {
			manager.shutDown();
		}
	}

}
