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

package org.eclipse.rdf4j.sail.nativerdf.benchmark;

import static org.eclipse.rdf4j.model.util.Values.iri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
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
 * @author Jeen Broekstra
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G" })
//@Fork(value = 1, jvmArgs = {"-Xms1G", "-Xmx1G", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SPARQLValuesClauseBenchmark {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private SailRepository repository;

	private static final String query_with_values_clause;
	private static final String query_without_values_clause;

	static {
		try {
			query_with_values_clause = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/query-values.qr"), StandardCharsets.UTF_8);

			query_without_values_clause = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/query-without-values.qr"), StandardCharsets.UTF_8);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws RunnerException, IOException, InterruptedException {
		Options opt = new OptionsBuilder()
				.include("SPARQLValuesClauseBenchmark") // adapt to run other benchmark tests
				// .addProfiler("stack", "lines=20;period=1;top=20")
				.forks(1)
				.build();

		new Runner(opt).run();

	}

	@Setup(Level.Trial)
	public void beforeClass() throws IOException, InterruptedException {
		tempDir.create();
		File file = tempDir.newFolder();

		repository = new SailRepository(new NativeStore(file, "spoc,posc,cspo,opsc"));

		int numberOfItems = 2;
		int numberOfChildren = 2;
		int numberOfTypeOwlClassStatements = 500;
		int numberOfSubClassOfStatements = 10_000;

		try (var conn = repository.getConnection()) {
			conn.begin(IsolationLevels.NONE);
			for (int i = 0; i < numberOfItems; i++) {

				var parent = iri("http://example.org/parent_" + i);

				Model m = new ModelBuilder().setNamespace(OWL.NS)
						.setNamespace("ex", "http://example.org/")
						.subject(parent)
						.add(RDF.TYPE, OWL.CLASS)
						.add(RDF.TYPE, RDFS.CLASS)
						.add(RDFS.LABEL, "parent " + i)
						.build();
				conn.add(m);
				if (i % 2 == 0) {
					for (int j = 0; j < numberOfChildren; j++) {
						m = new ModelBuilder().setNamespace(OWL.NS)
								.setNamespace("ex", "http://example.org/")
								.subject("ex:child_" + i + "_" + j)
								.add(RDF.TYPE, OWL.CLASS)
								.add(RDF.TYPE, RDFS.CLASS)
								.add(RDFS.SUBCLASSOF, parent)
								.add(RDFS.LABEL, "child of " + i)
								.build();
						conn.add(m);
					}
				}

			}
			for (int i = 0; i < numberOfTypeOwlClassStatements; i++) {
				conn.add(Values.bnode(), RDF.TYPE, OWL.CLASS);
			}

			for (int i = 0; i < numberOfSubClassOfStatements; i++) {
				conn.add(Values.bnode(), RDFS.SUBCLASSOF, Values.bnode());
			}
			conn.commit();
		}
	}

	@TearDown(Level.Trial)
	public void afterClass() {
		repository.shutDown();
	}

	@Benchmark
	public long valuesOptionalQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//
//			System.out.println("ONLY PARSED:");
//			System.out.println(connection.prepareTupleQuery(query_with_values_clause));
//			System.out.println("\nUNOPTIMIZED:");
//			System.out.println(
//					connection.prepareTupleQuery(query_with_values_clause).explain(Explanation.Level.Unoptimized));
//
//			System.out.println();
//			System.out.println("OPTIMIZED:");
//			System.out.println(
//					connection.prepareTupleQuery(query_with_values_clause).explain(Explanation.Level.Optimized));

//			return 0L;
			return connection
					.prepareTupleQuery(query_with_values_clause)
					.evaluate()
					.stream()
					.count();
		}
	}

	@Benchmark
	public long simpleEquivalentQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {

			return connection
					.prepareTupleQuery(query_without_values_clause)
					.evaluate()
					.stream()
					.count();
		}
	}

	/* private methods */

	private static InputStream getResourceAsStream(String filename) {
		return SPARQLValuesClauseBenchmark.class.getClassLoader().getResourceAsStream(filename);
	}
}
