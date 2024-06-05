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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;

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

}
