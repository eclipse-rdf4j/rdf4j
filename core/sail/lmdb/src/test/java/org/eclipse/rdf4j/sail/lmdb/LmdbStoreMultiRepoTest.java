/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test with multiple parallel repositories for {@link LmdbStore}.
 */
public class LmdbStoreMultiRepoTest {

	private File dataDir;

	private Locale defaultLocale;

	protected final ValueFactory F = SimpleValueFactory.getInstance();

	@BeforeEach
	public void before(@TempDir File dataDir) {
		defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);
		this.dataDir = dataDir;
	}

	@Test
	public void testRandomLoadManyRepositoriesInParallel() throws Exception {
		final int repositoryCount = 200;
		final int triplesPerRepository = 2000;
		final int triplesPerTransaction = 10;

		List<Repository> repositories = new ArrayList<>(repositoryCount);
		ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(200);
		ExecutorService executor = Executors.newCachedThreadPool();
		List<Callable<Void>> tasks = IntStream.range(0, repositoryCount)
				.mapToObj(repositoryIndex -> (Callable<Void>) () -> {
					File repositoryDir = new File(dataDir, "repo-" + repositoryIndex);
					Repository repository = new SailRepository(
							new LmdbStore(repositoryDir, new LmdbStoreConfig("spoc,posc")));
					repository.init();
					repositories.add(repository);
					scheduledExecutor.scheduleAtFixedRate(() -> {
						System.out.println("Repository " + repositoryIndex + ": Checking size...");
						try (RepositoryConnection connection = repository.getConnection()) {
							try (var result = connection.prepareTupleQuery(
									"select ?s (count(?o) as ?count) where { ?s ?p ?o } group by ?s")
									.evaluate()) {
								var count = result.stream().toList().size();
								System.out.println(
										"Repository " + repositoryIndex + ": Current size: " + connection.size()
												+ ", distinct subjects: " + count);
							}
						} catch (Exception e) {
							System.err.println(
									"Repository " + repositoryIndex + ": Error checking size: " + e.getMessage());
							e.printStackTrace(System.err);
						}
					}, 0, 1, TimeUnit.SECONDS);
					Random random = new Random(12345L + repositoryIndex);
					AtomicInteger tripleIndex = new AtomicInteger(0);

					int transactionCount = triplesPerRepository / triplesPerTransaction;
					System.out.println("Repository " + repositoryIndex + ": Adding " + triplesPerRepository
							+ " triples in " + transactionCount + " transactions of "
							+ triplesPerTransaction + " triples each.");
					List<Future<Void>> transactionFutures = new ArrayList<>();
					for (int i = 0; i < transactionCount; i++) {
						transactionFutures.add(executor.submit(() -> {
							try (RepositoryConnection transactionConnection = repository.getConnection()) {
								transactionConnection.begin();
								for (int triple = 0; triple < triplesPerTransaction; triple++) {
									Statement statement = createRandomStatement(random, tripleIndex.incrementAndGet());
									transactionConnection.add(statement);
								}
								transactionConnection.commit();
							}
							return null;
						}));
					}

					for (Future<Void> future : transactionFutures) {
						future.get();
					}

					System.out.println("Repository " + repositoryIndex + ": Verifying " + triplesPerRepository
							+ " triples.");
					try (RepositoryConnection verificationConnection = repository.getConnection()) {
						assertEquals(triplesPerRepository, verificationConnection.size());
					}
					return null;
				})
				.toList();
		try {
			List<Future<Void>> futures = executor.invokeAll(tasks);
			for (Future<Void> future : futures) {
				future.get();
			}
		} finally {
			scheduledExecutor.shutdown();
			executor.shutdown();
			assertTrue(scheduledExecutor.awaitTermination(10, TimeUnit.MINUTES));
			assertTrue(executor.awaitTermination(10, TimeUnit.MINUTES));
			repositories.forEach(Repository::shutDown);
		}
	}

	private Statement createRandomStatement(Random random, long tripleIndex) {
		IRI subject = F.createIRI("http://example.org/subjects/" + ((random.nextInt(1000) + 1) * 1000L + tripleIndex));
		IRI predicate = F.createIRI("http://example.org/predicates/" + (random.nextInt(20) + 1));
		Value object = switch (random.nextInt(3)) {
		case 0 -> F.createIRI("http://example.org/objects/" + (random.nextInt(2000) + 1));
		case 1 -> F.createLiteral("literal-" + random.nextInt(100000));
		default -> F.createBNode("bnode-" + (random.nextInt(100000) + tripleIndex));
		};
		return F.createStatement(subject, predicate, object);
	}

	@AfterEach
	public void after() {
		LmdbTestUtil.deleteDir(dataDir);
		Locale.setDefault(defaultLocale);
	}
}
