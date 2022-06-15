/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.memory.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
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
	public static final int CHUNKS = 10_000;

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
		Random random = new Random(4823924);
		for (MemStatement statement : statements) {
			statement.setTillSnapshot(Math.max(6, random.nextInt()));
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

		long count = Arrays.stream(memStatementList.getStatements()).filter(Objects::nonNull).count();
		assertEquals(statements.size(), count);
		assertEquals(statements.size(), memStatementList.size());
	}

	@Test
	@Timeout(120)
	public void addRemoveMultipleThreads() throws ExecutionException, InterruptedException {

		List<List<MemStatement>> partition = Lists.partition(statements, CHUNKS);
		ArrayList<MemStatement> shuffled = new ArrayList<>(statements);
		Collections.shuffle(shuffled, new Random(475839531));
		List<List<MemStatement>> partition2 = Lists.partition(shuffled, CHUNKS);

		MemStatementList memStatementList = new MemStatementList();

		ExecutorService executorService = Executors.newCachedThreadPool();
		try {

			List<? extends Future<?>> collect = Stream.concat(
					partition
							.stream()
							.map(l -> () -> {
								try {
									for (MemStatement statement : l) {
										memStatementList.add(statement);
									}
								} catch (InterruptedException e) {
									throw new IllegalStateException();
								}

							}),
					partition2
							.stream()
							.map(l -> (Runnable) () -> {
								try {
									for (MemStatement statement : l) {
										memStatementList.remove(statement);
									}
								} catch (InterruptedException e) {
									throw new IllegalStateException();
								}
							})
			)
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

	}

	@Test
	@Timeout(120)
	public void addRemoveConsistentMultipleThreads() throws ExecutionException, InterruptedException {

		List<List<MemStatement>> partition = Lists.partition(statements, CHUNKS);
		ArrayList<MemStatement> shuffled = new ArrayList<>(statements);
		Collections.shuffle(shuffled, new Random(475839531));
		List<List<MemStatement>> partition2 = Lists.partition(shuffled, CHUNKS);

		MemStatementList memStatementList = new MemStatementList();

		ExecutorService executorService = Executors.newCachedThreadPool();
		try {

			List<? extends Future<?>> collect = Stream.concat(
					partition
							.stream()
							.map(l -> () -> {
								try {
									for (MemStatement statement : l) {
										memStatementList.add(statement);
										statement.setTillSnapshot(5);
									}
								} catch (InterruptedException e) {
									throw new IllegalStateException();
								}
							}),
					partition2
							.stream()
							.map(l -> (Runnable) () -> {
								for (MemStatement statement : l) {
									while (statement.getTillSnapshot() != 5) {
										Thread.onSpinWait();
									}
									try {
										memStatementList.remove(statement);
									} catch (InterruptedException e) {
										throw new IllegalStateException(e);
									}
								}
							})
			)
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

		long count = Arrays.stream(memStatementList.getStatements()).filter(Objects::nonNull).count();
		assertEquals(0, memStatementList.size());
		assertEquals(0, count);

	}

	@Test
	@Timeout(120)
	public void addCleanSnapshotConsistentMultipleThreads() throws ExecutionException, InterruptedException {

		List<List<MemStatement>> partition = Lists.partition(statements, CHUNKS);

		List<Integer> tillSnapshots = partition.stream()
				.map(memStatements -> memStatements.stream().mapToInt(MemStatement::getTillSnapshot).max().getAsInt()
						+ 1)
				.collect(Collectors.toList());
		Collections.shuffle(tillSnapshots, new Random(4759354));

		MemStatementList memStatementList = new MemStatementList();
		ExecutorService executorService = Executors.newCachedThreadPool();
		AtomicInteger atomicInteger = new AtomicInteger(partition.size());
		try {

			List<? extends Future<?>> collect = Stream.concat(
					partition
							.stream()
							.map(l -> () -> {
								try {
									for (MemStatement statement : l) {
										memStatementList.add(statement);
									}
									atomicInteger.decrementAndGet();
								} catch (InterruptedException e) {
									throw new IllegalStateException();
								}
							}),
					tillSnapshots
							.stream()
							.map(cleanSnapshot -> (Runnable) () -> {
								try {
									while (atomicInteger.get() > 0) {
										memStatementList.cleanSnapshots(cleanSnapshot);
										Thread.yield();
									}

									memStatementList.cleanSnapshots(cleanSnapshot);
								} catch (InterruptedException e) {
									throw new IllegalStateException();
								}
							})
			)
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

		long count = Arrays.stream(memStatementList.getStatements()).filter(Objects::nonNull).count();
		assertEquals(0, memStatementList.size());
		assertEquals(0, count);

	}

}
