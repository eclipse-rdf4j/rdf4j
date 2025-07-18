/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.IntStream;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("slow")
public class ShutdownDuringValidationIT {

	private static final Logger logger = LoggerFactory
			.getLogger(ShutdownDuringValidationIT.class);

	private static final Model realData = getRealData();
	public static final int EXPECTED_REPOSITORY_SIZE = 613157;
	private static long MAX_MILLIS = Long.MIN_VALUE;

	private SailRepository repository;

	private static Model getRealData() {
		ClassLoader classLoader = ShutdownDuringValidationIT.class.getClassLoader();

		try {
			try (InputStream inputStream = new BufferedInputStream(
					classLoader.getResourceAsStream("complexBenchmark/datagovbe-valid.ttl"))) {
				return Rio.parse(inputStream, RDFFormat.TURTLE);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (NullPointerException e) {
			throw new RuntimeException("Could not load file: benchmarkFiles/datagovbe-valid.ttl", e);
		}
	}

	@BeforeAll
	static void beforeAll() throws IOException {
		for (int i = 0; i < 5; i++) {
			long start = System.currentTimeMillis();
			var repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.trig"));
			try {
				try (SailRepositoryConnection connection = repository.getConnection()) {
					connection.begin(ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation);
					connection.add(realData);
					connection.commit();
				}

			} finally {
				repository.shutDown();
			}
			MAX_MILLIS = Math.max(MAX_MILLIS, (long) ((System.currentTimeMillis() - start) * 1.1));
		}
	}

	@BeforeEach
	void setUp() throws IOException {
		assertFalse(Thread.interrupted());
		repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.trig"));
		((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);
	}

	@AfterEach
	void tearDown() {
		if (repository != null) {
			repository.shutDown();
			repository = null;
		}
	}

	@ParameterizedTest
	@MethodSource("sleepTimes")
	public void shutdownDuringValidation(int sleepMillis) {

		Thread thread;
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation);
			connection.add(realData);
			thread = startShutdownThread(sleepMillis);

			commitAndExpect(connection, EXPECTED_REPOSITORY_SIZE);

		}

		waitForThread(thread);

		try (SailRepositoryConnection connection = repository.getConnection()) {
			long size = connection.size();
			if (size > 0) {
				assertEquals(EXPECTED_REPOSITORY_SIZE, size,
						"The repository should either be empty or contain the expected data after shutdown during validation");
			} else {
				assertEquals(0, size, "The repository should be empty after shutdown during validation");
			}

		}

	}

	@ParameterizedTest
	@MethodSource("sleepTimes")
	public void shutdownDuringValidationTransactional(int sleepMillis) {

		Thread thread;
		try (var connection = repository.getConnection()) {
			connection.begin();
			ValueFactory vf = connection.getValueFactory();
			BNode bnode = vf.createBNode();
			connection.add(bnode, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}

		try (var connection = repository.getConnection()) {
			connection.begin(ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation);
			connection.add(realData);
			thread = startShutdownThread(sleepMillis);

			commitAndExpect(connection, EXPECTED_REPOSITORY_SIZE + 1);
		}

		waitForThread(thread);

		try (SailRepositoryConnection connection = repository.getConnection()) {
			long size = connection.size();

			assertThat(size)
					.as("Repository size")
					.isIn(0L, 1L, EXPECTED_REPOSITORY_SIZE + 1);

		}

	}

	@ParameterizedTest
	@MethodSource("sleepTimes")
	public void shutdownDuringValidationFailure(int sleepMillis) {

		Thread thread;

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation);
			connection.add(realData);
			ValueFactory vf = connection.getValueFactory();
			IRI iri = vf.createIRI("http://example.com/node1");
			connection.add(iri, RDF.TYPE, DCAT.DATASET);
			connection.add(iri, DCTERMS.ACCESS_RIGHTS, vf.createLiteral(""));
			thread = startShutdownThread(sleepMillis);

			commitAndExpect(connection, 0);
		}

		waitForThread(thread);

		try (SailRepositoryConnection connection = repository.getConnection()) {
			long size = connection.size();
			assertEquals(0, size, "The repository should be empty because the transaction always fails validation.");
		}

	}

	@ParameterizedTest
	@MethodSource("sleepTimes")
	public void shutdownDuringValidationFailureNonParallel(int sleepMillis) {

		Thread thread;

		SailRepositoryConnection connection = repository.getConnection();
		try (connection) {
			connection.begin(ShaclSail.TransactionSettings.PerformanceHint.SerialValidation);
			connection.add(realData);
			ValueFactory vf = connection.getValueFactory();
			IRI iri = vf.createIRI("http://example.com/node1");
			connection.add(iri, RDF.TYPE, DCAT.DATASET);
			connection.add(iri, DCTERMS.ACCESS_RIGHTS, vf.createLiteral(""));
			thread = startShutdownThread(sleepMillis);

			commitAndExpect(connection, 0);
		}

		waitForThread(thread);

		SailRepositoryConnection connection2 = repository.getConnection();
		try (connection2) {
			long size = connection2.size();
			assertEquals(0, size, "The repository should be empty because the transaction always fails validation.");
		}

	}

	@ParameterizedTest
	@MethodSource("sleepTimes")
	public void shutdownDuringValidationTransactionalNonParallel(int sleepMillis) {

		Thread thread;
		try (var connection = repository.getConnection()) {
			connection.begin();
			ValueFactory vf = connection.getValueFactory();
			BNode iri = vf.createBNode();
			connection.add(iri, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}

		try (var connection = repository.getConnection()) {
			connection.begin(ShaclSail.TransactionSettings.PerformanceHint.SerialValidation);
			connection.add(realData);
			thread = startShutdownThread(sleepMillis);

			commitAndExpect(connection, EXPECTED_REPOSITORY_SIZE + 1);
		}

		waitForThread(thread);

		try (SailRepositoryConnection connection = repository.getConnection()) {
			long size = connection.size();
			assertThat(size)
					.as("Repository size")
					.isIn(0L, 1L, EXPECTED_REPOSITORY_SIZE + 1);
		}

	}

	private static void commitAndExpect(SailRepositoryConnection connection, long expected) {
		try {
			connection.commit();
			assertFalse(Thread.currentThread().isInterrupted(), "The thread should not have been interrupted");
			long size = connection.size();
			assertEquals(expected, size, "The repository should be empty after shutdown during validation");
		} catch (RepositoryException ignored) {
			System.out.println(ignored.getMessage());
			connection.rollback();
		}
	}

	private static void waitForThread(Thread thread) {
		int i = 0;
		while (thread.isAlive() && i++ < 1000) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException ignored) {
			}
		}
	}

	private Thread startShutdownThread(int sleepMillis) {
		Thread thread;
		thread = new Thread(() -> {
			try {
				Thread.sleep(sleepMillis);
				repository.shutDown();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
		thread.start();
		return thread;
	}

	private static IntStream sleepTimes() {
		return IntStream.iterate(1, n -> n <= MAX_MILLIS, n -> n + 50);
	}
}
