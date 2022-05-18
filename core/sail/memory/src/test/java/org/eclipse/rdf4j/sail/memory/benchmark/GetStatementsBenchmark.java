/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.memory.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class GetStatementsBenchmark {

	private SailRepository repository;

	public static void main(String[] args) throws RunnerException, IOException, InterruptedException {
		Options opt = new OptionsBuilder()
				.include("GetStatementsBenchmark.*") // adapt to run other benchmark tests
				.forks(1)
				.build();

		new Runner(opt).run();

	}

	@Setup(Level.Trial)
	public void beforeClass() throws IOException, InterruptedException {
		repository = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			try (InputStream resourceAsStream = getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl")) {
				connection.add(resourceAsStream, RDFFormat.TURTLE);
			}
			connection.commit();
		}
	}

	@TearDown(Level.Trial)
	public void afterClass() {
		repository.shutDown();
	}

	@Benchmark
	public long groupByType() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			try (RepositoryResult<Statement> statements = connection.getStatements(null, RDF.TYPE, null)) {
				return statements
						.stream()
						.collect(Collectors.groupingBy(Statement::getObject, Collectors.counting()))
						.values()
						.stream()
						.mapToLong(l -> l)
						.max()
						.orElse(0);
			}
		}
	}

	@Benchmark
	public long groupByLanguage() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			try (RepositoryResult<Statement> statements = connection.getStatements(null, DCTERMS.LANGUAGE, null)) {
				return statements
						.stream()
						.collect(Collectors.groupingBy(Statement::getObject, Collectors.counting()))
						.values()
						.stream()
						.mapToLong(l -> l)
						.max()
						.orElse(0);
			}
		}
	}

	@Benchmark
	public long getTypesAndLabels() {
		long count = 0;

		try (SailRepositoryConnection connection = repository.getConnection()) {

			try (RepositoryResult<Statement> statements = connection.getStatements(null, RDF.TYPE, null)) {
				for (Statement statement : statements) {
					try (RepositoryResult<Statement> labels = connection.getStatements(statement.getSubject(),
							RDFS.LABEL, null)) {
						count += labels.stream().count();
					}
					if (statement.getObject().isResource()) {
						try (RepositoryResult<Statement> labels = connection
								.getStatements(((Resource) statement.getObject()), RDFS.LABEL, null)) {
							count += labels.stream().count();
						}
					}
				}
			}
		}

		return count;
	}

	private static InputStream getResourceAsStream(String filename) {
		return GetStatementsBenchmark.class.getClassLoader().getResourceAsStream(filename);
	}
}
