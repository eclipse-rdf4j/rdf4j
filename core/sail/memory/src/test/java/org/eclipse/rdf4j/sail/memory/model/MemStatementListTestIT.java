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

package org.eclipse.rdf4j.sail.memory.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.google.common.collect.Lists;

@Tag("slow")
public class MemStatementListTestIT {

	private static List<MemStatement> statements;
	public static final int CHUNKS = 1_000;

	@BeforeAll
	public static void beforeAll() throws IOException {
		MemoryStore memoryStore = new MemoryStore();
		try {
			try (NotifyingSailConnection connection = memoryStore.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				try (InputStream resourceAsStream = MemStatementList.class.getClassLoader()
						.getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl")) {
					Model parse = Rio.parse(resourceAsStream, RDFFormat.TURTLE);

					parse.stream()
							.sorted(Comparator.comparing(Object::toString))
							.forEach(statement -> {
								connection.addStatement(statement.getSubject(), statement.getPredicate(),
										statement.getObject(), statement.getContext());
							});
				}
				connection.commit();

				connection.begin(IsolationLevels.NONE);
				try (Stream<? extends Statement> stream = connection.getStatements(null, null, null, false).stream()) {
					statements = stream
							.map(s -> ((MemStatement) s))
							.sorted(Comparator.comparing(Object::toString))
							.limit(10000)
							.collect(Collectors.toList());
				}
				connection.commit();
			}
		} finally {
			memoryStore.shutDown();
		}
	}

	@BeforeEach
	public void beforeEach() {
		for (MemStatement statement : statements) {
			statement.setTillSnapshot(Integer.MAX_VALUE);
		}

	}

	@Test
	@Timeout(120)
	public void addMultipleThreads() throws ExecutionException, InterruptedException {

		List<List<MemStatement>> partition = Lists.partition(statements, CHUNKS);

		MemStatementList memStatementList = new MemStatementList();

		ExecutorService executorService = Executors.newCachedThreadPool();
		try {

			List<? extends Future<?>> collect = partition
					.stream()
					.map(l -> (Runnable) () -> {
						for (MemStatement statement : l) {
							try {
								memStatementList.add(statement);
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
						}
					})
					.collect(Collectors.toList())
					.stream()
					.map(executorService::submit)
					.collect(Collectors.toList());

			for (Future<?> future : collect) {
				future.get();
			}

		} finally {
			executorService.shutdownNow();
		}

		assertTrue(memStatementList.verifySizeForTesting());
		assertEquals(statements.size(), memStatementList.getRealSizeForTesting());
		assertEquals(statements.size(), memStatementList.size());
	}

	@Test
	@Timeout(120)
	public void addMultipleThreadsAndCleanupThread() throws ExecutionException, InterruptedException {

		List<List<MemStatement>> partition = Lists.partition(statements, CHUNKS);

		MemStatementList memStatementList = new MemStatementList();

		AtomicInteger snapshotVersion = new AtomicInteger(1);
		Random random = new Random();

		List<MemStatement> removedStatements = new LinkedList<>();

		CountDownLatch countDownLatch = new CountDownLatch(1);

		ExecutorService executorService = Executors.newCachedThreadPool();
		try {

			Future<?> cleanupFuture = executorService.submit(() -> {
				try {
					while (true) {
						try {

							if (Thread.interrupted()) {
								break;
							}

							int currentSnapshot = snapshotVersion.get();
							int highestUnusedTillSnapshot = currentSnapshot - 1;

							MemStatement[] statements = memStatementList.getStatements();

							/*
							 * The order of the statement list won't change from lastStmtPos down while we don't have
							 * the write lock (it might shrink or grow) as (1) new statements are always appended last,
							 * (2) we are the only process that removes statements, (3) this list is cleared on close.
							 */

							for (int i = statements.length - 1; i >= 0; i--) {
								if (Thread.currentThread().isInterrupted()) {
									break;
								}

								MemStatement st = statements[i];
								if (st == null) {
									continue;
								}

								if (st.getTillSnapshot() <= highestUnusedTillSnapshot) {
									// stale statement

									try {
										if (memStatementList.optimisticRemove(st, i)) {
											removedStatements.add(st);
										} else {
											System.err.println("Failed to remove!");
										}
									} catch (Error e) {
										System.err.println(e);
										throw e;
									}

								}

							}

						} catch (Throwable t) {
							if (t instanceof InterruptedException) {
								Thread.currentThread().interrupt();
							}
							fail(t);
						}
					}
				} finally {
					countDownLatch.countDown();
				}
			});

			List<? extends Future<?>> collect = partition
					.stream()
					.map(l -> (Runnable) () -> {
						try {
							for (MemStatement statement : l) {
								MemStatement memStatement = new MemStatement(statement.getSubject(),
										statement.getPredicate(), statement.getObject(), statement.getContext(),
										snapshotVersion.get());
								memStatementList.add(memStatement);
								assertEquals(Integer.MAX_VALUE, memStatement.getTillSnapshot());
								memStatement.setTillSnapshot(snapshotVersion.get() + 50);

								if (random.nextInt(10) == 0) {
									int i = snapshotVersion.incrementAndGet();
								}
							}
						} catch (InterruptedException e) {
							throw new IllegalStateException();
						}

					})
					.collect(Collectors.toList())
					.stream()
					.map(executorService::submit)
					.collect(Collectors.toList());

			for (Future<?> future : collect) {
				future.get();
			}

			boolean cancel = cleanupFuture.cancel(true);
			countDownLatch.await();

			boolean sizeIsCorrect = memStatementList.verifySizeForTesting();
			if (!sizeIsCorrect) {
				int realSize = memStatementList.getRealSizeForTesting();
				int size = memStatementList.size();
				assertEquals(realSize, size, "Expected calculated size should be equal to the actual size variable");
			}

			assertTrue(cleanupFuture.isDone() || cleanupFuture.isCancelled());
			assertEquals(statements.size(), removedStatements.size() + memStatementList.size());

			HashSet<MemStatement> removedStatementsSet = new HashSet<>(removedStatements);
			if (removedStatementsSet.size() < removedStatements.size()) {
				HashSet<MemStatement> temp = new HashSet<>(removedStatementsSet);
				for (MemStatement st : removedStatements) {
					if (!temp.contains(st)) {
						System.out.println();
					}
					temp.remove(st);
				}
			}
			assertEquals(removedStatementsSet.size(), removedStatements.size());

			List<MemStatement> collect1 = Arrays.stream(memStatementList.getStatements())
					.filter(Objects::nonNull)
					.collect(Collectors.toList());

			Set<MemStatement> memStatementsLeft = new HashSet<>(collect1);

			assertEquals(memStatementsLeft.size(), collect1.size());
			assertEquals(memStatementList.size(), memStatementsLeft.size());

			for (MemStatement memStatement : memStatementsLeft) {
				assertFalse(removedStatementsSet.contains(memStatement));
			}

			for (MemStatement memStatement : removedStatementsSet) {
				assertFalse(memStatementsLeft.contains(memStatement));
			}

			removedStatementsSet.addAll(memStatementsLeft);

			for (MemStatement statement : statements) {
				assertTrue(removedStatementsSet.contains(statement));
			}

		} finally {
			executorService.shutdownNow();
		}

	}

}
