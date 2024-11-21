/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ast.ContextWithShape;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class W3cComplianceTest {

	private final static Set<String> TESTS_FAILING_DUE_TO_MISSING_FEATURES_FROM_THE_SPEC = Set.of(
			"/core/node/xone-001.ttl",
			"/core/node/xone-duplicate.ttl",
			"/core/path/path-complex-001.ttl",
			"/core/path/path-oneOrMore-001.ttl",
			"/core/path/path-zeroOrMore-001.ttl",
			"/core/path/path-zeroOrOne-001.ttl",
			"/core/property/qualifiedMinCountDisjoint-001.ttl",
			"/core/property/qualifiedValueShapesDisjoint-001.ttl",
			"/core/property/uniqueLang-002.ttl"
	);

	public static Stream<Arguments> data() {
		return getTestFiles().stream()
				.sorted(Comparator.comparing(URL::toString))
				.map(Arguments::of);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void test(URL testCasePath) throws IOException {
		boolean testPassed = false;
		try {
			runTest(testCasePath);
			testPassed = true;
		} catch (AssertionError e) {
			if (e.toString().equals("org.opentest4j.AssertionFailedError: expected: <false> but was: <true>")) {
				testPassed = false;
			} else if (e.toString().equals("org.opentest4j.AssertionFailedError: expected: <true> but was: <false>")) {
				testPassed = false;
			} else {
				throw e;
			}
		} finally {
			String shortTestCasePath = testCasePath.toString().split("w3c")[1];
			if (testPassed) {
				assertThat(shortTestCasePath).isNotIn(TESTS_FAILING_DUE_TO_MISSING_FEATURES_FROM_THE_SPEC)
						.as("Test case %s was not expected to pass, but passed anyway. If you think that this makes sense because you've implemented more of the SHACL spec then just remove this test case from the TESTS_FAILING_DUE_TO_MISSING_FEATURES_FROM_THE_SPEC set",
								shortTestCasePath);
			} else {
				assertThat(shortTestCasePath).isIn(TESTS_FAILING_DUE_TO_MISSING_FEATURES_FROM_THE_SPEC)
						.as("Test case %s was not expected to fail.", shortTestCasePath);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("data")
	public void parsingTest(URL testCasePath) throws IOException, InterruptedException {
		runParsingTest(testCasePath);
	}

	private void runParsingTest(URL resourceName) throws IOException, InterruptedException {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		SailRepository sailRepository = new SailRepository(shaclSail);

		Utils.loadShapeData(sailRepository, resourceName, RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);

		Model statements = extractShapesModel(shaclSail);

		statements.setNamespace(RDF.NS);
		statements.setNamespace(SHACL.NS);

		sailRepository.shutDown();

		statements
				.filter(null, RDF.REST, null)
				.subjects()
				.forEach(s -> {
					int size = statements.filter(s, RDF.REST, null).objects().size();
					if (size > 1) {
						Rio.write(statements, System.out, RDFFormat.TURTLE,
								new WriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true));
					}
					assertEquals(size, 1, s + " has more than one rdf:rest");
				});

//		System.out.println(AbstractShaclTest.modelToString(statements));

		assert !statements.isEmpty();

	}

	private Model extractShapesModel(ShaclSail shaclSail) throws InterruptedException {
		List<ContextWithShape> shapes = shaclSail.getCachedShapes().getDataAndRelease();

		DynamicModel model = new DynamicModelFactory().createEmptyModel();

		HashSet<Resource> cycleDetection = new HashSet<>();

		shapes.forEach(shape -> shape.toModel(model, cycleDetection));

		return model;
	}

	private static Set<URL> getTestFiles() {

		Set<URL> testFiles = new HashSet<>();

		Deque<URL> manifests = new ArrayDeque<>();
		manifests.add(W3cComplianceTest.class.getClassLoader().getResource("w3c/core/manifest.ttl"));

		while (!manifests.isEmpty()) {

			URL pop = manifests.pop();
			Manifest manifest = new Manifest(pop);
			if (manifest.include.isEmpty()) {
				testFiles.add(pop);
			} else {
				manifests.addAll(manifest.include);
			}

		}

		return testFiles;

	}

	static class Manifest {

		List<URL> include;

		public Manifest(URL filename) {
			SailRepository sailRepository = new SailRepository(new MemoryStore());
			try (SailRepositoryConnection connection = sailRepository.getConnection()) {
				connection.add(filename, filename.toString(), RDFFormat.TRIG);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			try (SailRepositoryConnection connection = sailRepository.getConnection()) {
				try (Stream<Statement> stream = connection
						.getStatements(null,
								connection.getValueFactory()
										.createIRI("http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#include"),
								null)
						.stream()) {
					include = stream
							.map(Statement::getObject)
							.map(Value::stringValue)
							.map(v -> {
								try {
									return new URL(v);
								} catch (MalformedURLException e) {

									throw new RuntimeException(e);
								}
							})
							.collect(Collectors.toList());
				}
			}

		}

	}

	private void runTest(URL resourceName) throws IOException {
		W3C_shaclTestValidate expected = new W3C_shaclTestValidate(resourceName);

		SailRepository data = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.begin();
			connection.add(resourceName, "http://example.org/", RDFFormat.TRIG);
			connection.commit();
		}

		SailRepository shapes = new SailRepository(new MemoryStore());

		try (RepositoryConnection conn = shapes.getConnection()) {
			conn.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			conn.add(resourceName, resourceName.toString(), RDFFormat.TURTLE);
			conn.commit();
		}

		ValidationReport validate = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertEquals(expected.conforms, validate.conforms());
	}

	static class W3C_shaclTestValidate {

		W3C_shaclTestValidate(URL filename) {
			this.filename = filename.getPath();
			SailRepository sailRepository = Utils.getSailRepository(filename, RDFFormat.TURTLE);
			try (SailRepositoryConnection connection = sailRepository.getConnection()) {
				try (Stream<Statement> stream = connection.getStatements(null, SHACL.CONFORMS, null).stream()) {
					conforms = stream
							.map(Statement::getObject)
							.map(o -> (Literal) o)
							.map(Literal::booleanValue)
							.findFirst()
							.orElseThrow();
				}
			}
		}

		String filename;

		boolean conforms;
	}

}
