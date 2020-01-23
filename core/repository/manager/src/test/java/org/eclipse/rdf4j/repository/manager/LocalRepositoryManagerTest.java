/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for {@link LocalRepositoryManager}.
 * 
 * Note a lot of the functionality for the local manager can only be tested by integrating with other RDF4J modules -
 * more comprehensive tests are therefore located in LocalRepositoryManagerIntegationTest in the
 * rdfj4-repository-compliance module.
 * 
 * @author Jeen Broekstra
 */
public class LocalRepositoryManagerTest extends RepositoryManagerTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private File datadir;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	@Override
	public void setUp() throws Exception {
		datadir = tempDir.newFolder("local-repositorysubject-test");
		subject = new LocalRepositoryManager(datadir);
		subject.init();
	}

	/**
	 * @throws IOException if a problem occurs deleting temporary resources
	 */
	@After
	public void tearDown() throws IOException {
		subject.shutDown();
	}
}
