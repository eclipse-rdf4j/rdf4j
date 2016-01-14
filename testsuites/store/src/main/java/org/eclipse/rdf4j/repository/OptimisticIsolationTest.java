/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.OpenRDFException;
import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.optimistic.DeadLockTest;
import org.eclipse.rdf4j.repository.optimistic.DeleteInsertTest;
import org.eclipse.rdf4j.repository.optimistic.LinearTest;
import org.eclipse.rdf4j.repository.optimistic.ModificationTest;
import org.eclipse.rdf4j.repository.optimistic.MonotonicTest;
import org.eclipse.rdf4j.repository.optimistic.RemoveIsolationTest;
import org.eclipse.rdf4j.repository.optimistic.SailIsolationLevelTest;
import org.eclipse.rdf4j.repository.optimistic.SerializableTest;
import org.eclipse.rdf4j.repository.optimistic.SnapshotTest;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryFactory;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author James Leigh
 */
@RunWith(Suite.class)
@SuiteClasses({
		DeadLockTest.class,
		DeleteInsertTest.class,
		LinearTest.class,
		ModificationTest.class,
		RemoveIsolationTest.class,
		SailIsolationLevelTest.class,
		MonotonicTest.class,
		SnapshotTest.class,
		SerializableTest.class })
public abstract class OptimisticIsolationTest {

	private static RepositoryFactory factory;

	private static File dataDir;

	public static void setRepositoryFactory(RepositoryFactory factory)
		throws IOException
	{
		if (dataDir != null && dataDir.isDirectory()) {
			FileUtil.deleteDir(dataDir);
			dataDir = null;
		}
		OptimisticIsolationTest.factory = factory;
	}

	public static void setSailFactory(final SailFactory factory)
		throws IOException
	{
		setRepositoryFactory(new SailRepositoryFactory() {

			@Override
			public RepositoryImplConfig getConfig() {
				return new SailRepositoryConfig(factory.getConfig());
			}
		});
	}

	public static Repository getEmptyInitializedRepository(Class<?> caller)
		throws OpenRDFException, IOException
	{
		if (dataDir != null && dataDir.isDirectory()) {
			FileUtil.deleteDir(dataDir);
			dataDir = null;
		}
		dataDir = FileUtil.createTempDir(caller.getSimpleName());
		Repository repository = factory.getRepository(factory.getConfig());
		repository.setDataDir(dataDir);
		repository.initialize();
		RepositoryConnection con = repository.getConnection();
		try {
			con.clear();
			con.clearNamespaces();
		}
		finally {
			con.close();
		}
		return repository;
	}
}
