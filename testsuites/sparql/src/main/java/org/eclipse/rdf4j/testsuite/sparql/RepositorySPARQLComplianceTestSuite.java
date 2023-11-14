/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

/**
 * A suite of custom compliance tests on SPARQL query functionality for RDF4J Repositories.
 * <p>
 * To use this test suite, extend the abstract suite class, making sure that the correct {@link RepositoryFactory} gets
 * set on initialization, and torn down after. For example, to run the suite against an RDF4J Memory Store:
 *
 * <pre>
 * <code>
 * 	&#64;BeforeAll
	public static void setUpFactory() throws Exception {
		setRepositoryFactory(new SailRepositoryFactory() {
			&#64;Override
			public RepositoryImplConfig getConfig() {
				return new SailRepositoryConfig(new MemoryStoreFactory().getConfig());
			}
		});
	}

	&#64;AfterAll
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
@Experimental
public abstract class RepositorySPARQLComplianceTestSuite {
	@BeforeAll
	public static void setUpClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterAll
	public static void tearDownClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
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

	@Nested
	class AggregateTest extends org.eclipse.rdf4j.testsuite.sparql.tests.AggregateTest {
	}

	@Nested
	class ArbitraryLengthPathTest extends org.eclipse.rdf4j.testsuite.sparql.tests.ArbitraryLengthPathTest {
	}

	@Nested
	class BasicTest extends org.eclipse.rdf4j.testsuite.sparql.tests.BasicTest {
	}

	@Nested
	class BindTest extends org.eclipse.rdf4j.testsuite.sparql.tests.BindTest {
	}

	@Nested
	class BuiltinFunctionTest extends org.eclipse.rdf4j.testsuite.sparql.tests.BuiltinFunctionTest {
	}

	@Nested
	class ConstructTest extends org.eclipse.rdf4j.testsuite.sparql.tests.ConstructTest {
	}

	@Nested
	class DefaultGraphTest extends org.eclipse.rdf4j.testsuite.sparql.tests.DefaultGraphTest {
	}

	@Nested
	class DescribeTest extends org.eclipse.rdf4j.testsuite.sparql.tests.DescribeTest {
	}

	@Nested
	class GroupByTest extends org.eclipse.rdf4j.testsuite.sparql.tests.GroupByTest {
	}

	@Nested
	class InTest extends org.eclipse.rdf4j.testsuite.sparql.tests.InTest {
	}

	@Nested
	class OptionalTest extends org.eclipse.rdf4j.testsuite.sparql.tests.OptionalTest {
	}

	@Nested
	class PropertyPathTest extends org.eclipse.rdf4j.testsuite.sparql.tests.PropertyPathTest {
	}

	@Nested
	class SubselectTest extends org.eclipse.rdf4j.testsuite.sparql.tests.SubselectTest {
	}

	@Nested
	class UnionTest extends org.eclipse.rdf4j.testsuite.sparql.tests.UnionTest {
	}

	@Nested
	class ValuesTest extends org.eclipse.rdf4j.testsuite.sparql.tests.ValuesTest {
	}

	@Nested
	class OrderByTest extends org.eclipse.rdf4j.testsuite.sparql.tests.OrderByTest {
	}

	@Nested
	class ExistsTest extends org.eclipse.rdf4j.testsuite.sparql.tests.ExistsTest {
	}

	@Nested
	class MinusTest extends org.eclipse.rdf4j.testsuite.sparql.tests.MinusTest {
	}
}
