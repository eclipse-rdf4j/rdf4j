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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.exception.RDF4JException;
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
import org.eclipse.rdf4j.testsuite.sparql.tests.ExistsTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.FilterScopeTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.GroupByTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.InTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.MinusTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.OptionalTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.OrderByTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.PropertyPathTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.SubselectTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.UnionTest;
import org.eclipse.rdf4j.testsuite.sparql.tests.ValuesTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

/**
 * A suite of custom compliance tests on SPARQL query functionality for RDF4J Repositories.
 * <p>
 * To use this test suite, extend the abstract suite class, making sure that the correct {@link RepositoryFactory} gets
 * set on construction,
 *
 * @author Jeen Broekstra
 */
@Experimental
public abstract class RepositorySPARQLComplianceTestSuite {

	@TestFactory
	Stream<DynamicTest> aggregate() throws RDF4JException, IOException {
		return new AggregateTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> arbitraryLengthPath() throws RDF4JException, IOException {
		return new ArbitraryLengthPathTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> filterScopeTests() throws RDF4JException, IOException {
		return new FilterScopeTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> basic() throws RDF4JException, IOException {
		return new BasicTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> bind() throws RDF4JException, IOException {
		return new BindTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> builtinFunction() throws RDF4JException, IOException {
		return new BuiltinFunctionTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> construct() throws RDF4JException, IOException {
		return new ConstructTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> defaultGraph() throws RDF4JException, IOException {
		return new DefaultGraphTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> describe() throws RDF4JException, IOException {
		return new DescribeTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> groupBy() throws RDF4JException, IOException {
		return new GroupByTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> in() throws RDF4JException, IOException {
		return new InTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> optional() throws RDF4JException, IOException {
		return new OptionalTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> propertyPath() throws RDF4JException, IOException {
		return new PropertyPathTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> subselect() throws RDF4JException, IOException {
		return new SubselectTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> union() throws RDF4JException, IOException {
		return new UnionTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> values() throws RDF4JException, IOException {
		return new ValuesTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> orderBy() throws RDF4JException, IOException {
		return new OrderByTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> exists() throws RDF4JException, IOException {
		return new ExistsTest(this::getEmptyInitializedRepository).tests();
	}

	@TestFactory
	Stream<DynamicTest> minus() throws RDF4JException, IOException {
		return new MinusTest(this::getEmptyInitializedRepository).tests();
	}

	@BeforeAll
	public static void setUpClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterAll
	public static void tearDownClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	@TempDir
	private File dataDir;

	private static final AtomicInteger tempDirNameForRepoCounter = new AtomicInteger();

	protected final RepositoryFactory factory;

	public RepositorySPARQLComplianceTestSuite(RepositoryFactory factory) {
		super();
		this.factory = factory;
	}

	public Repository getEmptyInitializedRepository() {
		try {
			Repository repository = factory.getRepository(factory.getConfig());
			dataDir.mkdir();
			File tmpDirPerRepo = new File(dataDir, "tmpDirPerRepo" + tempDirNameForRepoCounter.getAndIncrement());
			if (!tmpDirPerRepo.mkdir()) {
				fail("Could not create temporary directory for test");
			}
			repository.setDataDir(tmpDirPerRepo);
			try (RepositoryConnection con = repository.getConnection()) {
				con.clear();
				con.clearNamespaces();
			}
			return repository;

		} catch (RDF4JException e) {
			fail(e);
			return null;
		}
	}
}
