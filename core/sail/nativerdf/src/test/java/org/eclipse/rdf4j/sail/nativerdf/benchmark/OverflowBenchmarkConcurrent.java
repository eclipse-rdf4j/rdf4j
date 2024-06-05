/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.nativerdf.benchmark;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G", "-XX:+UseG1GC" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class OverflowBenchmarkConcurrent {

	@Setup(Level.Trial)
	public void setup() {
		((Logger) (LoggerFactory
				.getLogger("org.eclipse.rdf4j.sail.nativerdf.MemoryOverflowModel")))
				.setLevel(ch.qos.logback.classic.Level.DEBUG);

		((Logger) (LoggerFactory
				.getLogger("org.eclipse.rdf4j.model.impl.AbstractMemoryOverflowModel")))
				.setLevel(ch.qos.logback.classic.Level.DEBUG);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("OverflowBenchmarkConcurrent") // adapt to run other benchmark tests
				.build();

		new Runner(opt).run();
	}

	@Benchmark
	public void manyConcurrentTransactions() throws IOException {
		File temporaryFolder = Files.newTemporaryFolder();
		SailRepository sailRepository = new SailRepository(new NotifySailWrapper(new NotifySailWrapper(
				new NotifySailWrapper(
						new NotifySailWrapper(new NotifySailWrapper(new NativeStore(temporaryFolder)))))));
		ExecutorService executorService = Executors.newFixedThreadPool(10);

		try {

			Model parse;
			try (InputStream resourceAsStream = getClass().getClassLoader()
					.getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl")) {
				parse = Rio.parse(resourceAsStream, RDFFormat.TURTLE);
			}

			List<Future<?>> futureList = new ArrayList<>();

			CountDownLatch countDownLatch = new CountDownLatch(1);

			for (int i = 0; i < 38; i++) {
				var seed = i + 485924;
				{
					Future<?> submit = executorService.submit(() -> {
						try {
							countDownLatch.await();
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						try (SailRepositoryConnection connection = sailRepository.getConnection()) {

							int addSize = new Random(seed).nextInt(parse.size());
							IRI context = Values.iri("http://example.org/" + new Random(seed + 1).nextInt(10));
							List<Statement> collect = parse.stream()
									.skip(addSize)
									.limit(10_000)
									.map(s -> SimpleValueFactory.getInstance()
											.createStatement(s.getSubject(), s.getPredicate(), s.getObject(), context))
									.collect(Collectors.toList());
							StringWriter stringWriter = new StringWriter();
							Rio.write(collect, stringWriter, RDFFormat.TRIG);
							String string = stringWriter.toString();

							connection.prepareUpdate("INSERT DATA { GRAPH " + string + " }").execute();

							System.out.println("Added");
						}
					});
					futureList.add(submit);
				}
				{
					Future<?> submit = executorService.submit(() -> {
						try {
							countDownLatch.await();
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						try (SailRepositoryConnection connection = sailRepository.getConnection()) {
							System.out.println("Waiting");
							long l = System.currentTimeMillis();
							while (!connection.isEmpty()) {
								try {
									Thread.sleep(1);
								} catch (InterruptedException e) {
									return;
								}
								if (System.currentTimeMillis() - l > 1000) {
									break;
								}
							}
							System.out.println("Removing");
							connection.begin();
							try (RepositoryResult<Statement> statements = connection.getStatements(null, null, null)) {
								statements.stream().limit(10_000).forEach(connection::remove);
							}
							connection.commit();

							System.out.println("Removed");
						}
					});
					futureList.add(submit);
				}
			}

			countDownLatch.countDown();

			for (int i = 0; i < futureList.size(); i++) {
				Future<?> future = futureList.get(i);
				try {
					future.get();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				System.out.println("Done: " + i);
			}

		} finally {
			try {
				executorService.shutdownNow();
			} finally {
				try {
					sailRepository.shutDown();
				} finally {
					FileUtils.deleteDirectory(temporaryFolder);
				}
			}
		}

	}

	static class NotifySailWrapper extends NotifyingSailWrapper {

		public NotifySailWrapper(NotifyingSail baseSail) {
			super(baseSail);
		}

		@Override
		public NotifyingSailConnection getConnection() throws SailException {
			return new Connection(super.getConnection());
		}
	}

	static class Connection extends NotifyingSailConnectionWrapper implements SailConnectionListener {

		Set<Statement> addedStatements = new HashSet<>();
		Set<Statement> removedStatements = new HashSet<>();

		public Connection(NotifyingSailConnection wrappedCon) {
			super(wrappedCon);
			addConnectionListener(this);
		}

		@Override
		public void statementAdded(Statement st) {
			removedStatements.remove(st);
			addedStatements.add(st);
		}

		@Override
		public void statementRemoved(Statement st) {
			addedStatements.remove(st);
			removedStatements.add(st);
		}

	}

}
