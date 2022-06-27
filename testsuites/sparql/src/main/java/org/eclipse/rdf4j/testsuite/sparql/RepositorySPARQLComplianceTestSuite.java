/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sparql;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.testsuite.sparql.tests.AggregateTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.ArbitraryLengthPathTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.BasicTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.BindTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.BuiltinFunctionTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.ConstructTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.DefaultGraphTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.DescribeTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.GroupByTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.InTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.OptionalTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.PropertyPathTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.SubselectTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.UnionTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.ValuesTest;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * A suite of custom compliance tests on SPARQL query functionality for RDF4J Repositories.
 * <p>
 * To use this test suite, extend the abstract suite class, making sure that the correct {@link RepositoryFactory} gets
 * set on initialization, and torn down after. For example, to run the suite against an RDF4J Memory Store:
 * 
 * <pre>
 * <code>
 * 	&#64;BeforeClass
	public static void setUpFactory() throws Exception {
		setRepositoryFactory(new SailRepositoryFactory() {
			&#64;Override
			public RepositoryImplConfig getConfig() {
				return new SailRepositoryConfig(new MemoryStoreFactory().getConfig());
			}
		});
	}

	&#64;AfterClass
	public static void tearDownFactory() throws Exception {
		setRepositoryFactory(null);
	}
 * </code>
 * </pre>
 * 
 * @author Jeen Broekstra
 * @implNote currently implemented as an abstract JUnit-4 suite. This suite is marked Experimental as we may want to
 *           make further improvements to its setup (including migrating to JUnit 5 when its suite support matures) in
 *           future minor releases.
 */
@RunWith(Suite.class)
@SuiteClasses({ AggregateTest.class, ArbitraryLengthPathTest.class, BasicTest.class, BindTest.class,
		BuiltinFunctionTest.class, ConstructTest.class, DefaultGraphTest.class, DescribeTest.class, GroupByTest.class,
		InTest.class, OptionalTest.class, PropertyPathTest.class, SubselectTest.class, UnionTest.class,
		ValuesTest.class })
@Experimental
public abstract class RepositorySPARQLComplianceTestSuite {
	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	private static RepositoryFactory factory;

	private static File dataDir;

	public static void setRepositoryFactory(RepositoryFactory factory) throws IOException {
		if (dataDir != null && dataDir.isDirectory()) {
			FileUtil.deleteDir(dataDir);
			dataDir = null;
		}
		RepositorySPARQLComplianceTestSuite.factory = factory;
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
