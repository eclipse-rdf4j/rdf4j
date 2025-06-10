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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.IntStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ShutdownDuringValidationTest {

	private static final Model realData = getRealData();
	public static final IRI DUMMY_PREDICATE = Values.iri("http://fjljfiwoejfoiwefiew/a");

	private SailRepository repository;

	private static Model getRealData() {
		ClassLoader classLoader = ShutdownDuringValidationTest.class.getClassLoader();

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

	@ParameterizedTest
	@MethodSource("sleepTimes")
	public void shutdownDuringValidation(int sleepMillis) throws InterruptedException {
		try {
			Thread thread;
			var repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.trig"));
			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation);
				connection.add(realData);
				thread = new Thread(() -> {
					try {
						Thread.sleep(sleepMillis);
						repository.shutDown();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				});
				thread.start();

				try {
					connection.commit();
					assertFalse(Thread.currentThread().isInterrupted(), "The thread should not have been interrupted");
					long size = connection.size();
					assertEquals(613157, size, "The repository should be empty after shutdown during validation");
				} catch (RepositoryException ignored) {
					connection.rollback();
				}
			}

			int i = 0;
			while (thread.isAlive() && i++ < 1000) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException ignored) {
				}
			}

			try (SailRepositoryConnection connection = repository.getConnection()) {
				long size = connection.size();
				if (size > 0) {
					assertEquals(613157, size,
							"The repository should either be empty or contain the expected data after shutdown during validation");
				} else {
					assertEquals(0, size, "The repository should be empty after shutdown during validation");
				}

			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (repository != null) {
				repository.shutDown();
			}
		}

	}

	private static IntStream sleepTimes() {
		return IntStream.iterate(1, n -> n <= 2000, n -> n + 10);
	}
}
