/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.testsuite.repository.optimistic.DeadLockTest;
import org.eclipse.rdf4j.testsuite.repository.optimistic.DeleteInsertTest;
import org.eclipse.rdf4j.testsuite.repository.optimistic.IsolationLevelTest;
import org.eclipse.rdf4j.testsuite.repository.optimistic.LinearTest;
import org.eclipse.rdf4j.testsuite.repository.optimistic.ModificationTest;
import org.eclipse.rdf4j.testsuite.repository.optimistic.MonotonicTest;
import org.eclipse.rdf4j.testsuite.repository.optimistic.RemoveIsolationTest;
import org.eclipse.rdf4j.testsuite.repository.optimistic.SerializableTest;
import org.eclipse.rdf4j.testsuite.repository.optimistic.SnapshotTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author James Leigh
 */
@RunWith(Suite.class)
@SuiteClasses({ DeadLockTest.class, DeleteInsertTest.class, LinearTest.class, ModificationTest.class,
		RemoveIsolationTest.class, IsolationLevelTest.class, MonotonicTest.class, SnapshotTest.class,
		SerializableTest.class })
public abstract class OptimisticIsolationTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	private static RepositoryFactory factory;

	private static File dataDir;

	public static void setRepositoryFactory(RepositoryFactory factory) throws IOException {
		if (dataDir != null && dataDir.isDirectory()) {
			FileUtil.deleteDir(dataDir);
			dataDir = null;
		}
		OptimisticIsolationTest.factory = factory;
	}

	public static Repository getEmptyInitializedRepository(Class<?> caller) throws RDF4JException, IOException {
		if (dataDir != null && dataDir.isDirectory()) {
			FileUtil.deleteDir(dataDir);
			dataDir = null;
		}
		dataDir = Files.createTempDirectory(caller.getSimpleName()).toFile();
		Repository repository = factory.getRepository(factory.getConfig());
		repository.setDataDir(dataDir);
		try (RepositoryConnection con = repository.getConnection()) {
			con.clear();
			con.clearNamespaces();
		}
		return repository;
	}
}
